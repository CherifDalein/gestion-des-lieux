package org.example.gestiondeslieux.data;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.model.*;
import org.example.gestiondeslieux.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PlaceRepository placeRepository;
    private final CollectionRepository collectionRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final CurrentLocationRepository currentLocationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // --- Roles ---
        Role roleUser  = roleRepository.save(new Role("ROLE_USER"));
        Role roleAdmin = roleRepository.save(new Role("ROLE_ADMIN"));

        // --- Users ---
        User alice = userRepository.save(User.builder()
                .username("alice").email("alice@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Alice").lastName("Dupont")
                .active(true).roles(Set.of(roleUser)).build());

        User bob = userRepository.save(User.builder()
                .username("bob").email("bob@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Bob").lastName("Martin")
                .active(true).roles(Set.of(roleUser)).build());

        userRepository.save(User.builder()
                .username("admin").email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin").lastName("System")
                .active(true).roles(Set.of(roleUser, roleAdmin)).build());

        // --- Places Alice (10) ---
        Place louvre = place("Musée du Louvre",     "Le plus grand musée d'art au monde",
                48.8606, 2.3376, List.of("paris", "musée", "art", "tourisme"), alice);
        place("Café de Paris",       "Un superbe café au cœur de Paris",
                48.8566, 2.3522, List.of("café", "paris", "restaurant"), alice);
        place("Tour Eiffel",           "Monument emblématique de Paris",
                48.8584, 2.2945, List.of("paris", "tourisme", "monument"), alice);
        place("Restaurant Le Comptoir","Excellent restaurant français traditionnel",
                48.8529, 2.3385, List.of("restaurant", "paris", "français"), alice);
        place("Jardins du Luxembourg", "Magnifiques jardins en plein cœur de Paris",
                48.8462, 2.3372, List.of("paris", "jardin", "promenade"), alice);
        place("Plage de Nice",         "Belle plage sur la Côte d'Azur",
                43.6957, 7.2713, List.of("plage", "nice", "mer", "vacances"), alice);
        place("Port Vieux de Marseille","Le vieux port de Marseille, très animé",
                43.2965, 5.3698, List.of("marseille", "port", "restaurant", "tourisme"), alice);
        place("Librairie Shakespeare", "Célèbre librairie anglophone à Paris",
                48.8527, 2.3472, List.of("paris", "librairie", "culture"), alice);
        place("Parc de la Tête d'Or",  "Grand parc urbain de Lyon avec zoo",
                45.7723, 4.8560, List.of("lyon", "parc", "promenade", "nature"), alice);
        place("Bouchon lyonnais Chez Paul","Authentique bouchon lyonnais",
                45.7600, 4.8320, List.of("lyon", "restaurant", "bouchon", "gastronomie"), alice);

        // --- Places Bob (5) ---
        place("Bureau Montparnasse",   "Mon lieu de travail principal",
                48.8417, 2.3228, List.of("travail", "paris"), bob);
        place("Salle de sport CrossFit","Ma salle de sport préférée",
                48.8701, 2.3429, List.of("sport", "paris"), bob);
        place("Supermarché Bio Coop",  "Supermarché bio de quartier",
                48.8762, 2.3590, List.of("courses", "bio", "paris"), bob);
        place("Café de la Paix",       "Café préféré pour travailler",
                48.8705, 2.3305, List.of("café", "travail", "paris"), bob);
        place("Parking Bercy",         "Parking pratique près de chez moi",
                48.8357, 2.3826, List.of("parking", "paris", "pratique"), bob);

        // --- Collections Alice ---
        Collection allPlaces = collectionRepository.save(Collection.builder()
                .name("Tous les lieux").tagFilter(null).user(alice).isShared(false).build());
        Collection colParis = collectionRepository.save(Collection.builder()
                .name("paris").tagFilter("paris").user(alice).isShared(false).build());
        for (String tag : List.of("café", "restaurant", "tourisme", "musée",
                "jardin", "plage", "nature", "lyon", "gastronomie")) {
            collectionRepository.save(Collection.builder()
                    .name(tag).tagFilter(tag).user(alice).isShared(false).build());
        }

        // --- Access Tokens ---
        accessTokenRepository.save(AccessToken.builder()
                .token("test-token-alice-paris-read")
                .label("Partage Paris avec Bob")
                .resourceType(ResourceType.COLLECTION)
                .resourceId(colParis.getId())
                .permission(Permission.READ)
                .expiresAt(null)
                .createdBy(alice)
                .accessCount(0L).build());

        accessTokenRepository.save(AccessToken.builder()
                .token("test-token-alice-louvre-write")
                .label("Accès écriture Louvre")
                .resourceType(ResourceType.PLACE)
                .resourceId(louvre.getId())
                .permission(Permission.WRITE)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdBy(alice)
                .accessCount(0L).build());

        accessTokenRepository.save(AccessToken.builder()
                .token("test-token-expired")
                .label("Token expiré (test)")
                .resourceType(ResourceType.COLLECTION)
                .resourceId(allPlaces.getId())
                .permission(Permission.READ)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .createdBy(alice)
                .accessCount(0L).build());

        // --- Position courante Bob ---
        currentLocationRepository.save(CurrentLocation.builder()
                .user(bob)
                .latitude(48.8600).longitude(2.3400)
                .accuracy(15.0)
                .timestamp(LocalDateTime.now())
                .isShared(true)
                .shareToken("test-location-bob-share").build());
    }

    private Place place(String title, String desc, double lat, double lon,
                        List<String> tags, User user) {
        return placeRepository.save(Place.builder()
                .title(title).description(desc)
                .latitude(lat).longitude(lon)
                .tags(tags).user(user).build());
    }
}
