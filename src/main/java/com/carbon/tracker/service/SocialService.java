package com.carbon.tracker.service;

import com.carbon.tracker.dto.LeaderboardEntry;
import com.carbon.tracker.model.User;
import com.carbon.tracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SocialService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActionService actionService;

    public List<LeaderboardEntry> getLeaderboard() {
        List<User> users = userRepository.findAll();
        
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        for (User user : users) {
            Double totalSaved = actionService.calculateTotalSavedCo2(user);
            // We initialize rank as 0, will assign after sorting
            entries.add(new LeaderboardEntry(user.getUsername(), totalSaved, 0));
        }

        // Sort by total saved descending
        entries.sort((a, b) -> Double.compare(b.getTotalSavedCo2(), a.getTotalSavedCo2()));

        // Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }
}
