package com.electrodiux.discordteams.teams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.yaml.snakeyaml.Yaml;

import com.electrodiux.discordteams.discord.Account;
import com.google.common.collect.Lists;

public class Team {

    private String name;
    private UUID uuid;

    private List<Account> members = Lists.newArrayList();

    public Team(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public static void saveTeams(List<Team> teams, File file) {
        Yaml yaml = new Yaml();

        try {
            yaml.dump(teams, new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Team> loadTeams(File file) {
        Yaml yaml = new Yaml();

        try {
            List<Team> teams = yaml.loadAs(new FileInputStream(file), List.class);
            return teams;
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
        }

        return Lists.newArrayList();
    }

}
