package com.example.pokedexapi;

/**
 * Model class representing a Pokemon with all relevant information
 * fetched from the PokeAPI
 */
public class Pokemon {
    private int id;
    private String name;
    private int weight;
    private int height;
    private int baseExperience;
    private String ability;
    private String move;
    private String imageUrl;

    public Pokemon(int id, String name, int weight, int height,
                   int baseExperience, String ability, String move, String imageUrl) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.height = height;
        this.baseExperience = baseExperience;
        this.ability = ability;
        this.move = move;
        this.imageUrl = imageUrl;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public int getHeight() {
        return height;
    }

    public int getBaseExperience() {
        return baseExperience;
    }

    public String getAbility() {
        return ability;
    }

    public String getMove() {
        return move;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return "#" + id + " - " + capitalize(name);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}