package org.exoplatform.wiki;

import org.hibernate.envers.AuditReaderFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

/**
 * Created by bdechateauvieux on 5/25/15.
 */
public class WikiDao {
    private final EntityManager em;

    public WikiDao(EntityManager em) {
        this.em = em;
    }

    public Wiki save(Wiki wiki) {
        return em.merge(wiki);
    }

    public Wiki find(Long id, int revision) {
        Wiki wiki = AuditReaderFactory.get(em).find(Wiki.class, id, revision);
        Page home = wiki.getWikiHome(); //Eagerly load wikiHome
        return wiki;
    }

    public Wiki find(Long id) {
        return em.find(Wiki.class, id);
    }

    public List<Number> getRevisions(Long id) {
        return AuditReaderFactory.get(em).getRevisions(Wiki.class, id);
    }
}
