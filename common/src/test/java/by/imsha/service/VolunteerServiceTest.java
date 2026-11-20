package by.imsha.service;

import api_specification.by.imsha.common.fusionauth.secured_client.api.FusionauthApiClient;
import api_specification.by.imsha.common.fusionauth.secured_client.model.SearchUsersRequest;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponse;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponseUsersInner;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponseUsersInnerData;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponseUsersInnerRegistrationsInner;
import by.imsha.properties.FusionauthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VolunteerServiceTest {
    private final FusionauthApiClient client = mock(FusionauthApiClient.class);
    private final VolunteerService service = new VolunteerService(client);

    private void configure() {
        FusionauthProperties properties = new FusionauthProperties();
        properties.setApplicationId("imsha");
        properties.setUserSearchPagination(2);
        ReflectionTestUtils.setField(service, "fusionAuthProperties", properties);
    }

    @Test
    void pagesUsersAndCountsOnlyVolunteersInTheConfiguredApplication() {
        configure();
        var first = new UsersSearchResponse().users(List.of(
                user("imsha", "Volunteer", "A", "B"),
                user("other", "Volunteer", "C")));
        var second = new UsersSearchResponse().users(List.of(
                user("imsha", "Volunteer", "A"), user("imsha", "Member", "D")));
        when(client.searchUsers(any(SearchUsersRequest.class)))
                .thenReturn(ResponseEntity.ok(first), ResponseEntity.ok(second),
                        ResponseEntity.ok(new UsersSearchResponse().users(List.of())));

        assertThat(service.getVolunteerNeededMap()).containsOnlyKeys("A", "B")
                .containsEntry("A", false).containsEntry("B", true);
        var captor = org.mockito.ArgumentCaptor.forClass(SearchUsersRequest.class);
        verify(client, org.mockito.Mockito.times(3)).searchUsers(captor.capture());
        assertThat(captor.getAllValues()).extracting(r -> r.getSearch().getStartRow()).containsExactly(0, 2, 4);
    }

    @Test
    void fusionAuthFailureReturnsTheDocumentedFalseFallback() {
        configure();
        when(client.searchUsers(any(SearchUsersRequest.class))).thenThrow(new IllegalStateException("unavailable"));

        assertThat(service.getVolunteerNeededMap().getOrDefault("A", true)).isFalse();
    }

    private static UsersSearchResponseUsersInner user(String application, String role, String... parishes) {
        var data = new UsersSearchResponseUsersInnerData();
        for (int i = 0; i < parishes.length; i++) data.putParishesItem(String.valueOf(i), parishes[i]);
        return new UsersSearchResponseUsersInner()
                .data(data)
                .registrations(List.of(new UsersSearchResponseUsersInnerRegistrationsInner()
                        .applicationId(application).roles(List.of(role))));
    }
}
