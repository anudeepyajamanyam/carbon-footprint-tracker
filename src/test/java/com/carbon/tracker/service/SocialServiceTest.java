package com.carbon.tracker.service;

import com.carbon.tracker.dto.LeaderboardEntry;
import com.carbon.tracker.model.Action;
import com.carbon.tracker.model.User;
import com.carbon.tracker.model.UserAction;
import com.carbon.tracker.repository.ActionRepository;
import com.carbon.tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SocialServiceTest {

    @Autowired
    private SocialService socialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private ActionService actionService;

    private User userA;
    private User userB;
    private Action testAction;

    @BeforeEach
    public void setUp() {
        userA = new User("usera_" + System.currentTimeMillis(), "usera@mail.com", "password", "ROLE_USER");
        userB = new User("userb_" + System.currentTimeMillis(), "userb@mail.com", "password", "ROLE_USER");
        userRepository.save(userA);
        userRepository.save(userB);

        testAction = new Action("Recycle Waste", "Recycle to save CO2", "WASTE", 5.0, "EASY");
        actionRepository.save(testAction);
    }

    @Test
    public void testGetLeaderboard_SortingAndRanking() {
        // userA completes challenge once (saves 5.0)
        UserAction uaA = actionService.optInToAction(userA, testAction.getId(), null);
        actionService.completeAction(userA, uaA.getId());

        // userB completes challenge twice (saves 10.0)
        UserAction uaB = actionService.optInToAction(userB, testAction.getId(), null);
        actionService.completeAction(userB, uaB.getId());
        // complete again on a new transaction or wait, completeAction enforces daily limit,
        // so to test higher savings for userB, let's complete it or check if totalCompletions can be updated.
        // Actually, let's just make userB complete a different action or manually set it.
        // Let's create another action worth 20.0 CO2 savings for userB to complete.
        Action highValueAction = new Action("Solar Panels", "Solar installation", "ENERGY", 20.0, "HARD");
        actionRepository.save(highValueAction);
        UserAction uaB2 = actionService.optInToAction(userB, highValueAction.getId(), null);
        actionService.completeAction(userB, uaB2.getId());

        List<LeaderboardEntry> leaderboard = socialService.getLeaderboard();
        assertFalse(leaderboard.isEmpty());

        // Find positions
        LeaderboardEntry first = leaderboard.get(0);
        assertEquals(userB.getUsername(), first.getUsername());
        assertEquals(25.0, first.getTotalSavedCo2(), 0.001); // 5.0 + 20.0 completed
        assertEquals(1, first.getRank());

        LeaderboardEntry second = leaderboard.stream()
                .filter(e -> e.getUsername().equals(userA.getUsername()))
                .findFirst().orElseThrow();
        assertEquals(5.0, second.getTotalSavedCo2(), 0.001);
    }
}
