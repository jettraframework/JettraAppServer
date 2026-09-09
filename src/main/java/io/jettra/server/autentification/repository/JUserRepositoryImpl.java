package io.jettra.server.autentification.repository;

import io.jettra.server.autentification.entity.JUser;
import io.jettra.server.db.security.JettraSecurityDB;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JUserRepositoryImpl implements JUserRepository {
    
    private final JettraSecurityDB db = new JettraSecurityDB();

    @Override
    public void save(JUser user) {
        if (user.id() == null) {
            user = new JUser(
                UUID.randomUUID(), 
                user.firstName(), 
                user.lastName(), 
                user.email(), 
                user.phone(), 
                user.active(), 
                user.jRoles(),
                user.assignedDatabases()
            );
        }
        db.save(user.id().toString(), user);
    }

    @Override
    public Optional<JUser> findById(UUID id) {
        if (id == null) return Optional.empty();
        return db.findById(JUser.class, id.toString());
    }

    @Override
    public List<JUser> findAll() {
   
        return db.findAll(JUser.class);
    }

    @Override
    public void delete(UUID id) {
        if (id != null) {
            db.delete(JUser.class, id.toString());
        }
    }

    @Override
    public List<JUser> search(String query) {
        return db.search(JUser.class, query);
    }
}
