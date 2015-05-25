import org.exoplatform.wiki.Page;
import org.exoplatform.wiki.Wiki;
import org.exoplatform.wiki.WikiDao;
import org.h2.tools.Server;
import org.junit.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.SQLException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by bdechateauvieux on 5/25/15.
 */
public class VersioningTest {
    private static final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("envers");
    private static Server webServer;

    private EntityManager em;

    /**
     * Simulate EM lifecycle (Session per Request pattern)
     */
    @Before
    public void createEntityManager() {
        em = entityManagerFactory.createEntityManager();
    }

    @After
    public void closeEntityManager() {
        em.close();
    }

//    @BeforeClass
//    public static void startH2Console() {
//        try {
//            System.out.println("H2 Web server - starting");
//            webServer = Server.createWebServer("-webAllowOthers", "-webPort", "8082").start();
//            System.out.println("H2 Web server - started");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        //Server server = Server.createTcpServer("-tcpAllowOthers","-tcpPort","9092").start();
//    }
//
//    @AfterClass
//    public static void stopH2Console() {
//        try {
//            Thread.sleep(10*60*1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        webServer.stop();
//    }

    @Test
    public void testNewVersionIsCreated() {
        //Given
        em.getTransaction().begin();
        WikiDao dao = new WikiDao(em);
        Wiki wiki = new Wiki();
        wiki = dao.save(wiki);
        em.getTransaction().commit();
        //When
        em.getTransaction().begin();
        wiki = dao.save(wiki);
        em.getTransaction().commit();
        //Then
        assertNotNull(dao.getRevisions(wiki.getId()));
        assertEquals(1, dao.getRevisions(wiki.getId()).size());
    }

    @Test
    public void testFindReturnsCurrentRevision() {
        //Given
        em.getTransaction().begin();
        WikiDao dao = new WikiDao(em);
        Wiki wiki = new Wiki();
        wiki.setName("original name");
        wiki.setOwner("original owner");
        wiki.setType("original type");
        wiki = dao.save(wiki);
        wiki.setName("new name");
        wiki.setOwner("new owner");
        wiki.setType("new type");
        wiki = dao.save(wiki);
        em.getTransaction().commit();
        //When
        wiki = dao.find(wiki.getId());
        //Then
        assertThat(wiki.getName(), is("new name"));
        assertThat(wiki.getOwner(), is("new owner"));
        assertThat(wiki.getType(), is("new type"));
    }

    @Test
    public void testOldInfosAreSaved() {
        //Given
        em.getTransaction().begin();
        WikiDao dao = new WikiDao(em);
        Wiki wiki = new Wiki();
        wiki.setName("original name");
        wiki.setOwner("original owner");
        wiki.setType("original type");
        wiki = dao.save(wiki);
        em.getTransaction().commit();
        em.getTransaction().begin();
        wiki.setName("new name");
        wiki.setOwner("new owner");
        wiki.setType("new type");
        wiki = dao.save(wiki);
        em.getTransaction().commit();
        //When
        Number revisionId = dao.getRevisions(wiki.getId()).get(0);
        wiki = dao.find(wiki.getId(),revisionId.intValue());
        //Then
        assertThat(wiki.getName(), is("original name"));
        assertThat(wiki.getOwner(), is("original owner"));
        assertThat(wiki.getType(), is("original type"));
    }

    @Test
     public void testVersioningTracksCascade() {
        //Given
        em.getTransaction().begin();
        WikiDao dao = new WikiDao(em);
        Page page = new Page();
        page.setName("Old page");
        Wiki wiki = new Wiki();
        wiki.setWikiHome(page);
        wiki = dao.save(wiki);
        em.getTransaction().commit();
        //When
        em.getTransaction().begin();
        page = new Page();
        page.setName("New page");
        wiki.setWikiHome(page);
        wiki = dao.save(wiki);
        em.getTransaction().commit();

        //Then
        assertNotNull(dao.getRevisions(wiki.getId()));
        assertEquals(2, dao.getRevisions(wiki.getId()).size());
        //New wikiPage is saved
        assertThat(dao.find(wiki.getId()).getWikiHome().getName(), is("New page"));
        //Old wikiPage is versioned
        Number revisionId = dao.getRevisions(wiki.getId()).get(0);
        assertThat(dao.find(wiki.getId(), revisionId.intValue()).getWikiHome().getName(), is("Old page"));

    }
}
