package com.competition.hub.controller;

import com.competition.hub.service.BlogService;
import com.competition.hub.service.ResourceService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.TeamService;
import com.competition.hub.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/overview")
public class OverviewController {
    private final ResourceService resourceService;
    private final BlogService blogService;
    private final SalonService salonService;
    private final TeamService teamService;
    private final UserService userService;

    public OverviewController(ResourceService resourceService, BlogService blogService, SalonService salonService, TeamService teamService, UserService userService) {
        this.resourceService = resourceService;
        this.blogService = blogService;
        this.salonService = salonService;
        this.teamService = teamService;
        this.userService = userService;
    }

    @GetMapping
    public Map<String, Long> overview() {
        return Map.of(
                "resources", resourceService.countApproved(),
                "blogs", blogService.count(),
                "salons", salonService.count(),
                "teams", teamService.count(),
                "users", userService.count()
        );
    }
}
