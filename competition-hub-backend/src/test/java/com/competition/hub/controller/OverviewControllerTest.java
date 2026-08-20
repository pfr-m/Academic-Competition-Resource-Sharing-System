package com.competition.hub.controller;

import com.competition.hub.service.BlogService;
import com.competition.hub.service.ResourceService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.TeamService;
import com.competition.hub.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OverviewControllerTest {

    @Test
    void overviewUsesApprovedResourceCountInsteadOfRawTotal() {
        ResourceService resourceService = mock(ResourceService.class);
        BlogService blogService = mock(BlogService.class);
        SalonService salonService = mock(SalonService.class);
        TeamService teamService = mock(TeamService.class);
        UserService userService = mock(UserService.class);
        OverviewController controller = new OverviewController(resourceService, blogService, salonService, teamService, userService);

        when(resourceService.countApproved()).thenReturn(2L);
        when(blogService.count()).thenReturn(2L);
        when(salonService.count()).thenReturn(2L);
        when(teamService.count()).thenReturn(2L);
        when(userService.count()).thenReturn(5L);

        Map<String, Long> result = controller.overview();

        assertThat(result.get("resources")).isEqualTo(2L);
        assertThat(result.get("blogs")).isEqualTo(2L);
        assertThat(result.get("salons")).isEqualTo(2L);
        assertThat(result.get("teams")).isEqualTo(2L);
        assertThat(result.get("users")).isEqualTo(5L);
    }
}
