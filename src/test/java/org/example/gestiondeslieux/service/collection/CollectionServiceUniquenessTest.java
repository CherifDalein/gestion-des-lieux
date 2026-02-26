package org.example.gestiondeslieux.service.collection;

import org.example.gestiondeslieux.exceptions.AlreadyExistsException;
import org.example.gestiondeslieux.model.Collection;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.CollectionRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.CreateCollectionRequest;
import org.example.gestiondeslieux.request.UpdateCollectionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CollectionServiceUniquenessTest {

    @Autowired
    private ICollectionService collectionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Test
    void createCollection_should_reject_duplicate_non_null_tag_filter() {
        User user = createUser();
        collectionRepository.save(Collection.builder()
                .name("Paris")
                .tagFilter("paris")
                .user(user)
                .isShared(false)
                .build());

        CreateCollectionRequest req = new CreateCollectionRequest();
        req.setName("Dup Paris");
        req.setTagFilter("paris");

        assertThrows(AlreadyExistsException.class, () -> collectionService.createCollection(req, user.getId()));
    }

    @Test
    void createCollection_should_reject_duplicate_null_tag_filter() {
        User user = createUser();
        collectionRepository.save(Collection.builder()
                .name("Tous les lieux")
                .tagFilter(null)
                .user(user)
                .isShared(false)
                .build());

        CreateCollectionRequest req = new CreateCollectionRequest();
        req.setName("Dup All Places");
        req.setTagFilter(null);

        assertThrows(AlreadyExistsException.class, () -> collectionService.createCollection(req, user.getId()));
    }

    @Test
    void updateCollection_should_reject_duplicate_tag_filter() {
        User user = createUser();
        collectionRepository.save(Collection.builder()
                .name("Paris")
                .tagFilter("paris")
                .user(user)
                .isShared(false)
                .build());
        Collection tourisme = collectionRepository.save(Collection.builder()
                .name("Tourisme")
                .tagFilter("tourisme")
                .user(user)
                .isShared(false)
                .build());

        UpdateCollectionRequest req = new UpdateCollectionRequest();
        req.setTagFilter("paris");

        assertThrows(AlreadyExistsException.class, () -> collectionService.updateCollection(tourisme.getId(), req, user.getId()));
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .email("collection-uniq-" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .active(true)
                .build());
    }
}
