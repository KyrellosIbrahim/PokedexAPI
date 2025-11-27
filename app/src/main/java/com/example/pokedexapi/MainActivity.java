package com.example.pokedexapi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText pokemonInput;
    private Button addButton, clearButton, clearAllButton;
    private ListView watchlistView;
    private ImageView pokemonImage;
    private TextView pokemonName, pokemonId, pokemonWeight, pokemonHeight;
    private TextView pokemonBaseXP, pokemonAbility, pokemonMove;

    private ArrayList<Pokemon> watchlist;
    private ArrayAdapter<Pokemon> adapter;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());

        initializeViews();

        // Initialize data structures
        watchlist = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, watchlist);
        watchlistView.setAdapter(adapter);

        // Set up event listeners
        setupListeners();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        pokemonInput = findViewById(R.id.pokemonInput);
        addButton = findViewById(R.id.addButton);
        clearButton = findViewById(R.id.clearButton);
        clearAllButton = findViewById(R.id.clearAllButton);
        watchlistView = findViewById(R.id.watchlistView);
        pokemonImage = findViewById(R.id.pokemonImage);
        pokemonName = findViewById(R.id.pokemonName);
        pokemonId = findViewById(R.id.pokemonId);
        pokemonWeight = findViewById(R.id.pokemonWeight);
        pokemonHeight = findViewById(R.id.pokemonHeight);
        pokemonBaseXP = findViewById(R.id.pokemonBaseXP);
        pokemonAbility = findViewById(R.id.pokemonAbility);
        pokemonMove = findViewById(R.id.pokemonMove);
    }

    /**
     * Set up all button and list click listeners
     */
    private void setupListeners() {
        // Add button - validates and adds Pokemon to watchlist
        addButton.setOnClickListener(v -> {
            String input = pokemonInput.getText().toString().trim();
            if (validateInput(input)) {
                fetchAndAddPokemon(input.toLowerCase());
            }
        });

        // Clear button - clears current profile and search bar
        clearButton.setOnClickListener(v -> clearCurrentProfile());

        // Clear All button - removes all Pokemon from watchlist
        clearAllButton.setOnClickListener(v -> clearAllPokemon());

        // ListView item click - displays selected Pokemon profile
        watchlistView.setOnItemClickListener((parent, view, position, id) -> {
            Pokemon selectedPokemon = watchlist.get(position);
            displayPokemonProfile(selectedPokemon);
        });
    }

    /**
     * Validates the user input for Pokemon name or ID
     * @param input The user input string
     * @return true if valid, false otherwise
     */
    private boolean validateInput(String input) {
        if (input.isEmpty()) {
            showToast("Please enter a Pokemon name or ID");
            return false;
        }

        // Check for invalid characters
        String invalidChars = "%&*(@)!;:<>";
        for (char c : invalidChars.toCharArray()) {
            if (input.indexOf(c) != -1) {
                showToast("Invalid character detected: " + c);
                return false;
            }
        }

        // If input is numeric, validate range
        try {
            int pokemonId = Integer.parseInt(input);
            if (pokemonId < 0 || pokemonId > 1025) {
                showToast("Pokemon ID must be between 0 and 1025");
                return false;
            }
        } catch (NumberFormatException e) {
            // Input is a name, which is valid
        }

        return true;
    }

    /**
     * Fetches Pokemon data from PokeAPI and adds to watchlist
     * @param input Pokemon name or ID
     */
    private void fetchAndAddPokemon(String input) {
        executorService.execute(() -> {
            try {
                // Construct API URL
                String apiUrl = "https://pokeapi.co/api/v2/pokemon/" + input;
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // Check response code
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    JSONObject jsonObject = new JSONObject(response.toString());
                    Pokemon pokemon = parsePokemonData(jsonObject);

                    // Update UI on main thread
                    mainHandler.post(() -> {
                        // Check if Pokemon already exists in watchlist
                        boolean exists = false;
                        for (Pokemon p : watchlist) {
                            if (p.getId() == pokemon.getId()) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            watchlist.add(pokemon);
                            adapter.notifyDataSetChanged();
                            showToast("Added " + capitalize(pokemon.getName()) + " to watchlist");
                            displayPokemonProfile(pokemon);
                            pokemonInput.setText("");
                        } else {
                            showToast("Pokemon already in watchlist");
                        }
                    });
                } else {
                    mainHandler.post(() -> showToast("Pokemon not found"));
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> showToast("Error fetching Pokemon data"));
            }
        });
    }

    /**
     * Parses JSON data from PokeAPI into Pokemon object
     * @param jsonObject JSON response from API
     * @return Pokemon object with parsed data
     */
    private Pokemon parsePokemonData(JSONObject jsonObject) throws Exception {
        int id = jsonObject.getInt("id");
        String name = jsonObject.getString("name");
        int weight = jsonObject.getInt("weight");
        int height = jsonObject.getInt("height");
        int baseExperience = jsonObject.optInt("base_experience", 0);

        // Get first ability
        JSONArray abilities = jsonObject.getJSONArray("abilities");
        String ability = "";
        if (abilities.length() > 0) {
            ability = abilities.getJSONObject(0)
                    .getJSONObject("ability")
                    .getString("name");
        }

        // Get first move
        JSONArray moves = jsonObject.getJSONArray("moves");
        String move = "";
        if (moves.length() > 0) {
            move = moves.getJSONObject(0)
                    .getJSONObject("move")
                    .getString("name");
        }

        // Get sprite image URL (using high quality sprite for extra credit)
        // Using official artwork instead of basic sprite
        String imageUrl = jsonObject.getJSONObject("sprites")
                .getJSONObject("other")
                .getJSONObject("official-artwork")
                .getString("front_default");

        return new Pokemon(id, name, weight, height, baseExperience, ability, move, imageUrl);
    }

    /**
     * Displays Pokemon profile in the UI
     * @param pokemon The Pokemon to display
     */
    private void displayPokemonProfile(Pokemon pokemon) {
        pokemonName.setText("Name: " + capitalize(pokemon.getName()));
        pokemonId.setText("Pokedex ID: #" + pokemon.getId());
        pokemonWeight.setText("Weight: " + pokemon.getWeight() + " hectograms");
        pokemonHeight.setText("Height: " + pokemon.getHeight() + " decimeters");
        pokemonBaseXP.setText("Base XP: " + pokemon.getBaseExperience());
        pokemonAbility.setText("Ability: " + capitalize(pokemon.getAbility().replace("-", " ")));
        pokemonMove.setText("Move: " + capitalize(pokemon.getMove().replace("-", " ")));

        // Load image from URL
        loadImageFromUrl(pokemon.getImageUrl());
    }

    /**
     * Loads and displays image from URL
     * @param imageUrl URL of the Pokemon sprite
     */
    private void loadImageFromUrl(String imageUrl) {
        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                mainHandler.post(() -> pokemonImage.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> pokemonImage.setImageResource(android.R.drawable.ic_menu_help));
            }
        });
    }

    /**
     * Clears the current Pokemon profile and search input (Extra Credit)
     */
    private void clearCurrentProfile() {
        pokemonInput.setText("");
        pokemonName.setText("");
        pokemonId.setText("");
        pokemonWeight.setText("");
        pokemonHeight.setText("");
        pokemonBaseXP.setText("");
        pokemonAbility.setText("");
        pokemonMove.setText("");
        pokemonImage.setImageBitmap(null);
        showToast("Profile cleared");
    }

    /**
     * Clears all Pokemon from the watchlist (Extra Credit)
     */
    private void clearAllPokemon() {
        watchlist.clear();
        adapter.notifyDataSetChanged();
        clearCurrentProfile();
        showToast("All Pokemon removed from watchlist");
    }

    /**
     * Displays a Toast message
     * @param message The message to display
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Capitalizes the first letter of each word in a string
     * @param str The string to capitalize
     * @return Capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                capitalized.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return capitalized.toString().trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}