-- Seed data for Eco-Friendly Actions/Challenges
INSERT INTO action (id, title, description, category, co2_savings, difficulty) VALUES 
(1, 'Use Public Transit', 'Commute via bus, subway, or train instead of driving your car.', 'TRANSPORT', 4.5, 'MEDIUM'),
(2, 'Meatless Monday', 'Skip meat for a day and enjoy a plant-based diet.', 'FOOD', 2.8, 'EASY'),
(3, 'Switch to LED Bulbs', 'Replace old incandescent lightbulbs with energy-efficient LED ones.', 'ENERGY', 0.8, 'EASY'),
(4, 'Unplug Standby Devices', 'Unplug chargers, TVs, and electronics when not in use to stop phantom loads.', 'ENERGY', 0.5, 'EASY'),
(5, 'Compost Organic Waste', 'Compost your food scraps and organic waste instead of throwing them in landfill trash.', 'WASTE', 1.2, 'MEDIUM'),
(6, 'Walk or Bike Short Trips', 'Walk or bicycle for all trips under 3 kilometers.', 'TRANSPORT', 1.5, 'EASY'),
(7, 'Zero Waste Grocery Shopping', 'Use reusable bags, containers, and buy in bulk to eliminate packaging waste.', 'WASTE', 0.9, 'MEDIUM'),
(8, 'Optimize Home Thermostat', 'Lower your heating by 1-2 degrees in winter or raise AC in summer.', 'ENERGY', 2.2, 'MEDIUM'),
(9, 'Adopt a Strict Vegan Diet', 'Transition to a fully plant-based diet for maximum carbon savings.', 'FOOD', 6.5, 'HARD'),
(10, 'Install Solar Panels', 'Transition your home energy usage to renewable solar power.', 'ENERGY', 12.0, 'HARD');
