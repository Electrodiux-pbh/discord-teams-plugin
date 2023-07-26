package com.electrodiux.discordteams.team;

import javax.annotation.Nonnull;

public interface TeamEditor {

    @Nonnull
    String getName();

    void sendMessage(String message);

    boolean hasPermission(@Nonnull TeamPermission permission);

    boolean hasHigherPriorityThan(@Nonnull TeamMember other);

}
