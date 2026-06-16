package com.carbon.tracker.service;

import com.carbon.tracker.dto.ActivityLogRequest;
import com.carbon.tracker.model.Action;
import com.carbon.tracker.model.User;
import com.carbon.tracker.model.UserAction;
import com.carbon.tracker.repository.ActionRepository;
import com.carbon.tracker.repository.UserRepository;
import com.carbon.tracker.repository.UserActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ActionServiceTest {

    @Autowired
    private ActionService actionService;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActionRepository userActionRepository;

    private User testUser;
    private Action testAction;

    @BeforeEach
    public void setUp() {
        testUser = new User("actionuser_" + System.currentTimeMillis(), "actionuser@mail.com", "password", "ROLE_USER");
        userRepository.save(testUser);

        testAction = new Action("Plant a tree", "Plant one tree for carbon offset", "OFFSET", 10.0, "HARD");
        actionRepository.save(testAction);
    }

    @Test
    public void testGetAllActions() {
        List<Action> actions = actionService.getAllActions();
        assertFalse(actions.isEmpty());
        assertTrue(actions.stream().anyMatch(a -> a.getTitle().equals("Plant a tree")));
    }

    @Test
    public void testOptInToAction_Success() {
        UserAction userAction = actionService.optInToAction(testUser, testAction.getId(), null);
        assertNotNull(userAction);
        assertEquals("ACTIVE", userAction.getStatus());
        assertEquals(testUser.getId(), userAction.getUser().getId());
        assertEquals(testAction.getId(), userAction.getAction().getId());
    }

    @Test
    public void testOptInToAction_DuplicateGuard() {
        actionService.optInToAction(testUser, testAction.getId(), null);
        assertThrows(IllegalArgumentException.class, () -> actionService.optInToAction(testUser, testAction.getId(), null));
    }

    @Test
    public void testCompleteAction_StreakAndMetrics() {
        UserAction userAction = actionService.optInToAction(testUser, testAction.getId(), null);
        
        UserAction completed = actionService.completeAction(testUser, userAction.getId());
        assertNotNull(completed);
        assertEquals(1, completed.getTotalCompletions());
        assertEquals(1, completed.getStreakDays());
        assertEquals(LocalDate.now(), completed.getCompletionDate());

        // Try completing again on the same day (should throw error)
        assertThrows(IllegalArgumentException.class, () -> actionService.completeAction(testUser, userAction.getId()));
    }

    @Test
    public void testCompleteAction_Unauthorized() {
        User otherUser = new User("other_" + System.currentTimeMillis(), "other@mail.com", "password", "ROLE_USER");
        userRepository.save(otherUser);

        UserAction userAction = actionService.optInToAction(testUser, testAction.getId(), null);

        assertThrows(SecurityException.class, () -> actionService.completeAction(otherUser, userAction.getId()));
    }

    @Test
    public void testAbandonAction() {
        UserAction userAction = actionService.optInToAction(testUser, testAction.getId(), null);
        Long id = userAction.getId();

        actionService.abandonAction(testUser, id);
        assertFalse(userActionRepository.findById(id).isPresent());
    }

    @Test
    public void testCalculateTotalSavedCo2() {
        UserAction userAction = actionService.optInToAction(testUser, testAction.getId(), null);
        actionService.completeAction(testUser, userAction.getId());

        Double saved = actionService.calculateTotalSavedCo2(testUser);
        assertEquals(10.0, saved, 0.001);
    }
}
