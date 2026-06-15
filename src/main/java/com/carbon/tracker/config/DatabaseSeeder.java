package com.carbon.tracker.config;

import com.carbon.tracker.model.Action;
import com.carbon.tracker.repository.ActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Programmatic database seeder component.
 * Executes on application startup and seeds the action/eco-challenges catalog.
 * By using this in place of standard data.sql, we ensure compatibility with H2 file-based mode,
 * query efficiency, database independence, and prevent primary key conflicts on successive restarts.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Autowired
    private ActionRepository actionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the actions catalog is empty
        if (actionRepository.count() == 0) {
            logger.info("Initializing actions catalog database seeding...");

            List<Action> actions = Arrays.asList(
                new Action("Use Public Transit", "Commute via bus, subway, or train instead of driving your car.", "TRANSPORT", 4.5, "MEDIUM"),
                new Action("Meatless Monday", "Skip meat for a day and enjoy a plant-based diet.", "FOOD", 2.8, "EASY"),
                new Action("Switch to LED Bulbs", "Replace old incandescent lightbulbs with energy-efficient LED ones.", "ENERGY", 0.8, "EASY"),
                new Action("Unplug Standby Devices", "Unplug chargers, TVs, and electronics when not in use to stop phantom loads.", "ENERGY", 0.5, "EASY"),
                new Action("Compost Organic Waste", "Compost your food scraps and organic waste instead of throwing them in landfill trash.", "WASTE", 1.2, "MEDIUM"),
                new Action("Walk or Bike Short Trips", "Walk or bicycle for all trips under 3 kilometers.", "TRANSPORT", 1.5, "EASY"),
                new Action("Zero Waste Grocery Shopping", "Use reusable bags, containers, and buy in bulk to eliminate packaging waste.", "WASTE", 0.9, "MEDIUM"),
                new Action("Optimize Home Thermostat", "Lower your heating by 1-2 degrees in winter or raise AC in summer.", "ENERGY", 2.2, "MEDIUM"),
                new Action("Adopt a Strict Vegan Diet", "Transition to a fully plant-based diet for maximum carbon savings.", "FOOD", 6.5, "HARD"),
                new Action("Install Solar Panels", "Transition your home energy usage to renewable solar power.", "ENERGY", 12.0, "HARD")
            );

            // Assign IDs manually to align exactly with client-side maps (1 to 10)
            long id = 1;
            for (Action action : actions) {
                action.setId(id++);
            }

            actionRepository.saveAll(actions);
            logger.info("Successfully seeded actions database catalog with {} eco-challenges.", actions.size());
        } else {
            logger.info("Actions database catalog already populated. Seeding skipped.");
        }
    }
}
