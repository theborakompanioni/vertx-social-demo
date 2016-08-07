package com.github.theborakompanioni.authorizer;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.authorization.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class UsernameAuthorizer implements Authorizer {

    private List<String> usernames;

    public UsernameAuthorizer(String username) {
        this(Collections.singletonList(requireNonNull(username)));
    }

    public UsernameAuthorizer(String... usernames) {
        this(Arrays.asList(usernames));
    }

    public UsernameAuthorizer(List<String> usernames) {
        this.usernames = ImmutableList.copyOf(requireNonNull(usernames));
    }

    public boolean isAuthorized(WebContext context, UserProfile profile) {
        return Optional.ofNullable(profile)
                .filter(p -> p instanceof CommonProfile)
                .map(p -> (CommonProfile) p)
                .map(CommonProfile::getUsername)
                .flatMap(currentUserName -> this.usernames.stream()
                        .filter(name -> StringUtils.equals(currentUserName, name))
                        .findAny())
                .isPresent();
    }
}