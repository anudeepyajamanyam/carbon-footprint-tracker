# deploy-aws.ps1
# Set error action preference to stop on errors
$ErrorActionPreference = "Stop"

$region = "us-east-1"
$dbIdentifier = "biometrck-db"
$secretName = "biometrck/prod/credentials"
$appName = "carbon-footprint-tracker"
$envName = "biometrck-prod-env"
$s3Bucket = "elasticbeanstalk-us-east-1-512856247000"
$jarName = "carbon-footprint-tracker-1.0.0.jar"
$jarPath = "target/$jarName"

Write-Host "--- Starting BiomeTrck AWS Deployment ---" -ForegroundColor Cyan

# 1. AWS IAM Role setup for Secrets Manager access
Write-Host "Configuring IAM instance profile role permissions for Secrets Manager..." -ForegroundColor Green
$policyJson = @{
    Version = "2012-10-17"
    Statement = @(
        @{
            Effect = "Allow"
            Action = @("secretsmanager:GetSecretValue")
            Resource = "arn:aws:secretsmanager:${region}:*:secret:biometrck/prod/credentials-*"
        }
    )
} | ConvertTo-Json -Depth 5 -Compress

# Save policy document temporarily
$policyFile = "secrets-policy.json"
$policyJson | Out-File -FilePath $policyFile -Encoding ascii

try {
    aws iam put-role-policy --role-name aws-elasticbeanstalk-ec2-role --policy-name SecretsManagerAccessForBiomeTrck --policy-document "file://$policyFile" --no-cli-pager
    Write-Host "Attached Secrets Manager permission policy to aws-elasticbeanstalk-ec2-role" -ForegroundColor Green
} finally {
    if (Test-Path $policyFile) { Remove-Item $policyFile -Force }
}

# 2. Database Setup: Create RDS PostgreSQL if it doesn't exist
Write-Host "Checking if RDS PostgreSQL database instance '$dbIdentifier' exists..." -ForegroundColor Green
$dbExists = $false
try {
    $dbCheck = aws rds describe-db-instances --db-instance-identifier $dbIdentifier --region $region --no-cli-pager 2>&1
    if ($LASTEXITCODE -eq 0) {
        $dbExists = $true
        Write-Host "Database '$dbIdentifier' already exists." -ForegroundColor Yellow
    }
} catch {
    # Instance doesn't exist
}

$dbPassword = ""
if (-not $dbExists) {
    # Generate random strong password
    $dbPassword = [Guid]::NewGuid().ToString().Replace("-", "").Substring(0, 16) + "aB1!"
    Write-Host "Creating new PostgreSQL database instance (db.t3.micro)... This may take 5-10 minutes..." -ForegroundColor Green
    aws rds create-db-instance `
        --db-instance-identifier $dbIdentifier `
        --db-instance-class db.t3.micro `
        --engine postgres `
        --master-username postgres `
        --master-user-password $dbPassword `
        --allocated-storage 20 `
        --publicly-accessible `
        --region $region `
        --no-cli-pager
}

# 3. Wait for Database to be Available
Write-Host "Waiting for database instance to become available..." -ForegroundColor Green
while ($true) {
    $dbStatus = (aws rds describe-db-instances --db-instance-identifier $dbIdentifier --region $region --query "DBInstances[0].DBInstanceStatus" --output text --no-cli-pager)
    Write-Host "Current Database Status: $dbStatus" -ForegroundColor Gray
    if ($dbStatus -eq "available") {
        break
    }
    Start-Sleep -Seconds 15
}

# Retrieve Endpoint URL
$dbHost = $(aws rds describe-db-instances --db-instance-identifier $dbIdentifier --region $region --query "DBInstances[0].Endpoint.Address" --output text --no-cli-pager).Trim()
$dbUrl = "jdbc:postgresql://${dbHost}:5432/postgres"
Write-Host "Database is available at: $dbHost" -ForegroundColor Green

# 4. Save credentials to Secrets Manager
Write-Host "Saving configuration details to AWS Secrets Manager..." -ForegroundColor Green
$secretExists = $false
try {
    aws secretsmanager describe-secret --secret-id $secretName --region $region --no-cli-pager 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $secretExists = $true
    }
} catch {}

# If creating database or secret didn't exist, generate password/token
if ([string]::IsNullOrEmpty($dbPassword)) {
    # Secret exists, but let's make sure we update it or fetch the password.
    try {
        $existingSecretJson = (aws secretsmanager get-secret-value --secret-id $secretName --region $region --query "SecretString" --output text --no-cli-pager)
        $existingSecret = $existingSecretJson | ConvertFrom-Json
        $dbPassword = $existingSecret.SPRING_DATASOURCE_PASSWORD
    } catch {
        # Fallback to random if not found
        $dbPassword = [Guid]::NewGuid().ToString().Replace("-", "").Substring(0, 16) + "aB1!"
    }
}

$jwtSecret = [Guid]::NewGuid().ToString().Replace("-", "") + [Guid]::NewGuid().ToString().Replace("-", "")
$secretPayload = @{
    SPRING_DATASOURCE_URL = $dbUrl
    SPRING_DATASOURCE_USERNAME = "postgres"
    SPRING_DATASOURCE_PASSWORD = $dbPassword
    JWT_SECRET = $jwtSecret
} | ConvertTo-Json

# Write to temporary file using UTF-8 without BOM to avoid JSON parsing issues in Java
$secretFile = "secret-payload-temp.json"
[System.IO.File]::WriteAllText((Resolve-Path .).Path + "/" + $secretFile, $secretPayload, (New-Object System.Text.UTF8Encoding($false)))

try {
    if (-not $secretExists) {
        aws secretsmanager create-secret --name $secretName --description "BiomeTrck production database and credentials" --secret-string "file://$secretFile" --region $region --no-cli-pager
        Write-Host "Created new secret: $secretName" -ForegroundColor Green
    } else {
        aws secretsmanager put-secret-value --secret-id $secretName --secret-string "file://$secretFile" --region $region --no-cli-pager
        Write-Host "Updated secret: $secretName" -ForegroundColor Green
    }
} finally {
    if (Test-Path $secretFile) { Remove-Item $secretFile -Force }
}

# 5. Open security group inbound rules for PostgreSQL
Write-Host "Configuring Security Group rules to allow PostgreSQL connections..." -ForegroundColor Green
$defaultSg = (aws ec2 describe-security-groups --filters "Name=group-name,Values=default" --region $region --query "SecurityGroups[0].GroupId" --output text --no-cli-pager)
try {
    aws ec2 authorize-security-group-ingress --group-id $defaultSg --protocol tcp --port 5432 --cidr 0.0.0.0/0 --region $region --no-cli-pager 2>&1
    Write-Host "Opened port 5432 in default security group" -ForegroundColor Green
} catch {
    Write-Host "Port 5432 is already open or could not be verified (skipping)" -ForegroundColor Yellow
}

# 6. Upload Application JAR to S3
Write-Host "Uploading application JAR to S3..." -ForegroundColor Green
if (-not (Test-Path $jarPath)) {
    throw "Application JAR file not found at $jarPath. Please build the project first."
}
aws s3 cp $jarPath "s3://$s3Bucket/$jarName" --region $region --no-cli-pager

# 7. Create/Update Elastic Beanstalk Application & Environment
Write-Host "Creating/Updating Elastic Beanstalk Application..." -ForegroundColor Green
$appExists = $false
try {
    aws elasticbeanstalk describe-applications --application-names $appName --region $region --no-cli-pager 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { $appExists = $true }
} catch {}

if (-not $appExists) {
    aws elasticbeanstalk create-application --application-name $appName --region $region --no-cli-pager 2>&1 | Out-Null
}

$versionLabel = "v-" + (Get-Date -Format "yyyyMMdd-HHmmss")
Write-Host "Creating Application Version: $versionLabel" -ForegroundColor Green
aws elasticbeanstalk create-application-version `
    --application-name $appName `
    --version-label $versionLabel `
    --source-bundle S3Bucket=$s3Bucket,S3Key=$jarName `
    --region $region `
    --no-cli-pager

# Save Elastic Beanstalk configuration settings to options.json
$options = @(
    @{ Namespace = "aws:elasticbeanstalk:environment"; OptionName = "EnvironmentType"; Value = "SingleInstance" }
    @{ Namespace = "aws:autoscaling:launchconfiguration"; OptionName = "IamInstanceProfile"; Value = "aws-elasticbeanstalk-ec2-role" }
    @{ Namespace = "aws:autoscaling:launchconfiguration"; OptionName = "InstanceType"; Value = "t3.micro" }
    @{ Namespace = "aws:elasticbeanstalk:application:environment"; OptionName = "PORT"; Value = "5000" }
    @{ Namespace = "aws:elasticbeanstalk:application:environment"; OptionName = "SPRING_PROFILES_ACTIVE"; Value = "prod" }
    @{ Namespace = "aws:elasticbeanstalk:application:environment"; OptionName = "AWS_REGION"; Value = $region }
)
$optionsFile = "options.json"
$options | ConvertTo-Json -Depth 5 -Compress | Out-File -FilePath $optionsFile -Encoding ascii

Write-Host "Checking if Elastic Beanstalk environment '$envName' exists..." -ForegroundColor Green
$envExists = $false
try {
    $envCheck = aws elasticbeanstalk describe-environments --application-name $appName --environment-names $envName --region $region --no-cli-pager 2>&1
    $envStatus = (aws elasticbeanstalk describe-environments --application-name $appName --environment-names $envName --region $region --query "Environments[0].Status" --output text --no-cli-pager)
    if ($envStatus -ne "None" -and $envStatus -ne "") {
        $envExists = $true
    }
} catch {}

if (-not $envExists) {
    Write-Host "Creating Elastic Beanstalk Environment: $envName (this takes a few minutes)..." -ForegroundColor Green
    aws elasticbeanstalk create-environment `
        --application-name $appName `
        --environment-name $envName `
        --version-label $versionLabel `
        --solution-stack-name "64bit Amazon Linux 2023 v4.12.2 running Corretto 21" `
        --option-settings "file://$optionsFile" `
        --region $region `
        --no-cli-pager
} else {
    Write-Host "Updating Elastic Beanstalk Environment: $envName..." -ForegroundColor Green
    aws elasticbeanstalk update-environment `
        --environment-name $envName `
        --version-label $versionLabel `
        --option-settings "file://$optionsFile" `
        --region $region `
        --no-cli-pager
}

# Cleanup options file
if (Test-Path $optionsFile) { Remove-Item $optionsFile -Force }

# 8. Wait for Environment to become Green
Write-Host "Waiting for environment to complete deployment..." -ForegroundColor Green
while ($true) {
    $statusCheck = (aws elasticbeanstalk describe-environments --environment-names $envName --region $region --query "Environments[0]" --output json --no-cli-pager | ConvertFrom-Json)
    $envStatus = $statusCheck.Status
    $envHealth = $statusCheck.Health
    $envUrl = $statusCheck.CNAME
    
    Write-Host "Environment Status: $envStatus | Health: $envHealth" -ForegroundColor Gray
    if ($envStatus -eq "Ready") {
        Write-Host "BiomeTrck successfully deployed to AWS!" -ForegroundColor Green
        Write-Host "Live URL: http://$envUrl" -ForegroundColor Cyan -BackgroundColor DarkBlue
        break
    }
    Start-Sleep -Seconds 15
}
