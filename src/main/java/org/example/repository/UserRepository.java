package org.example.repository;

import org.example.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public class UserRepository {

    private final EntityManager em;

    public UserRepository(EntityManager em) {
        this.em = em;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    public Optional<User> findByEmail(String email) {
        try {
            return Optional.of(
                em.createQuery("SELECT u FROM User u WHERE u.email = :e", User.class)
                  .setParameter("e", email)
                  .getSingleResult()
            );
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
                 .getResultList();
    }

    public List<User> search(String term) {
        String like = "%" + term.toLowerCase() + "%";
        return em.createQuery(
            "SELECT u FROM User u WHERE LOWER(u.email) LIKE :t OR LOWER(u.firstName) LIKE :t OR LOWER(u.lastName) LIKE :t",
            User.class).setParameter("t", like).getResultList();
    }

    public List<User> findByStatus(String status) {
        return em.createQuery("SELECT u FROM User u WHERE u.status = :s", User.class)
                 .setParameter("s", status).getResultList();
    }

    public List<User> findExpiredSubscriptions() {
        return em.createQuery(
            "SELECT u FROM User u WHERE u.isPremium = true AND u.subscriptionExpiry < :now",
            User.class).setParameter("now", LocalDateTime.now()).getResultList();
    }

    public long countAll() {
        return em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
    }

    public long countByStatus(String status) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.status = :s", Long.class)
                 .setParameter("s", status).getSingleResult();
    }

    public long countPremium() {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.isPremium = true", Long.class)
                 .getSingleResult();
    }

    public void save(User user) {
        if (user.getId() == null) em.persist(user);
        else em.merge(user);
    }

    public void delete(User user) {
        User managed = em.contains(user) ? user : em.merge(user);
        em.remove(managed);
    }
    /** Legacy overload — delegates to the full version with no role/sort. */
    public List<User> findAdvanced(String search, String status, String plan, int page, int pageSize) {
        return findAdvanced(search, status, null, plan, null, page, pageSize);
    }

    /** Full advanced search with role filter and sort. */
    public List<User> findAdvanced(String search, String status, String role,
                                   String plan, String sort, int page, int pageSize) {
        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1");
        if (search != null && !search.isBlank())
            jpql.append(" AND (LOWER(u.email) LIKE :search OR LOWER(u.firstName) LIKE :search OR LOWER(u.lastName) LIKE :search)");
        if (status != null && !status.isBlank())
            jpql.append(" AND u.status = :status");
        if (role != null && !role.isBlank())
            jpql.append(" AND u.roles LIKE :role");
        if (plan != null && !plan.isBlank())
            jpql.append(" AND u.subscriptionPlan = :plan");
        jpql.append(switch (sort != null ? sort : "") {
            case "name"    -> " ORDER BY u.firstName ASC, u.lastName ASC";
            case "email"   -> " ORDER BY u.email ASC";
            case "status"  -> " ORDER BY u.status ASC";
            case "premium" -> " ORDER BY u.isPremium DESC, u.subscriptionPlan ASC";
            default        -> " ORDER BY u.createdAt DESC";
        });
        var query = em.createQuery(jpql.toString(), User.class);
        if (search != null && !search.isBlank())
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (status != null && !status.isBlank())
            query.setParameter("status", status);
        if (role != null && !role.isBlank())
            query.setParameter("role", "%\"" + role + "\"%");
        if (plan != null && !plan.isBlank())
            query.setParameter("plan", plan);
        return query.setFirstResult((page - 1) * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
    }

    /** Count matching records for pagination. */
    public long countAdvanced(String search, String status, String role, String plan) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(u) FROM User u WHERE 1=1");
        if (search != null && !search.isBlank())
            jpql.append(" AND (LOWER(u.email) LIKE :search OR LOWER(u.firstName) LIKE :search OR LOWER(u.lastName) LIKE :search)");
        if (status != null && !status.isBlank())
            jpql.append(" AND u.status = :status");
        if (role != null && !role.isBlank())
            jpql.append(" AND u.roles LIKE :role");
        if (plan != null && !plan.isBlank())
            jpql.append(" AND u.subscriptionPlan = :plan");
        var query = em.createQuery(jpql.toString(), Long.class);
        if (search != null && !search.isBlank())
            query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (status != null && !status.isBlank())
            query.setParameter("status", status);
        if (role != null && !role.isBlank())
            query.setParameter("role", "%\"" + role + "\"%");
        if (plan != null && !plan.isBlank())
            query.setParameter("plan", plan);
        return query.getSingleResult();
    }

    public List<User> findExpiringSubscriptions(LocalDateTime before) {
        return em.createQuery(
                        "SELECT u FROM User u WHERE u.isPremium = true AND u.subscriptionExpiry < :before",
                        User.class)
                .setParameter("before", before)
                .getResultList();
    }
}
