package net.ent.etnc.game_arena;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.Role;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seed de données de démo. Tourne au démarrage si la base est vide.
 * Idempotent : vérifie qu'il n'y a pas déjà d'utilisateurs avant d'insérer.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, QuizRepository quizRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return; // déjà initialisé

        createUser("admin", "admin", Role.ADMIN);
        createUser("tanguy", "1595", Role.USER);
        createUser("gloo", "1234", Role.USER);

        quizRepository.save(buildGeographie());
        quizRepository.save(buildSciences());
        quizRepository.save(buildHistoire());
        quizRepository.save(buildSport());
        quizRepository.save(buildCinema());
        quizRepository.save(buildMusique());
        quizRepository.save(buildGastronomie());
        quizRepository.save(buildAnimaux());
        quizRepository.save(buildMythologie());
        quizRepository.save(buildTechno());
        quizRepository.save(buildLitterature());
        quizRepository.save(buildAstronomie());
    }

    private void createUser(String username, String password, Role role) {
        User u = new User(); u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password)); u.setRole(role);
        userRepository.save(u);
    }

    private Quiz quiz(String titre, String description) {
        Quiz q = new Quiz(); q.setTitre(titre); q.setDescription(description); return q;
    }

    private Question question(String text, String bonne, String... mauvaises) {
        Question q = new Question(); q.setText(text); q.setType(TypeQuestion.CHOIX);
        List<Reponse> all = new ArrayList<>();
        all.add(reponse(bonne, true));
        for (String m : mauvaises) all.add(reponse(m, false));
        Collections.shuffle(all);
        for (Reponse r : all) q.addReponse(r);
        return q;
    }

    private Question questionLibre(String text, String bonne) {
        Question q = new Question(); q.setText(text); q.setType(TypeQuestion.SAISIE_LIBRE);
        q.addReponse(reponse(bonne, true)); return q;
    }

    private Reponse reponse(String text, boolean estBonne) {
        Reponse r = new Reponse(); r.setText(text); r.setEstBonne(estBonne); return r;
    }

    private Quiz buildGeographie() {
        Quiz q = quiz("Géographie mondiale", "Capitales, fleuves et pays du monde entier");
        q.addQuestion(question("Quelle est la capitale de la France ?", "Paris", "Lyon", "Marseille", "Bordeaux"));
        q.addQuestion(question("Quel est le plus grand pays du monde en superficie ?", "Russie", "Canada", "États-Unis", "Chine"));
        q.addQuestion(question("Quel fleuve traverse Le Caire ?", "Le Nil", "Le Congo", "Le Niger", "L'Euphrate"));
        q.addQuestion(question("Dans quel pays se trouve le Machu Picchu ?", "Pérou", "Mexique", "Bolivie", "Colombie"));
        q.addQuestion(question("Quelle est la capitale de l'Australie ?", "Canberra", "Sydney", "Melbourne", "Brisbane"));
        q.addQuestion(question("Quel est le plus long fleuve d'Amérique du Sud ?", "L'Amazone", "Le Paraná", "L'Orénoque", "Le São Francisco"));
        q.addQuestion(question("Dans quel pays se trouvent les pyramides de Gizeh ?", "Égypte", "Soudan", "Maroc", "Tunisie"));
        q.addQuestion(question("Quelle est la capitale du Japon ?", "Tokyo", "Osaka", "Kyoto", "Hiroshima"));
        q.addQuestion(question("Quel est le plus petit pays du monde ?", "Le Vatican", "Monaco", "Nauru", "Saint-Marin"));
        q.addQuestion(question("Quelle chaîne de montagnes sépare l'Europe de l'Asie ?", "L'Oural", "Les Alpes", "Les Carpates", "Les Balkans"));
        q.addQuestion(question("Quel est le plus grand désert du monde ?", "L'Antarctique", "Le Sahara", "Le Gobi", "L'Arabie"));
        q.addQuestion(questionLibre("Comment s'appelle le plus haut sommet du monde ?", "Everest"));
        return q;
    }

    private Quiz buildSciences() {
        Quiz q = quiz("Sciences et nature", "Physique, chimie, biologie et mathématiques");
        q.addQuestion(question("Quelle est la formule chimique de l'eau ?", "H₂O", "H₂O₂", "CO₂", "NaCl"));
        q.addQuestion(question("Combien de chromosomes possède une cellule humaine normale ?", "46", "23", "48", "42"));
        q.addQuestion(question("Quel est le symbole chimique de l'or ?", "Au", "Or", "Go", "Ag"));
        q.addQuestion(question("Quelle planète est surnommée la planète rouge ?", "Mars", "Jupiter", "Mercure", "Saturne"));
        q.addQuestion(question("Quelle est la vitesse de la lumière (approximation) ?", "300 000 km/s", "150 000 km/s", "3 000 km/s", "30 000 km/s"));
        q.addQuestion(question("Quel gaz les plantes absorbent-elles pour la photosynthèse ?", "CO₂", "O₂", "N₂", "H₂"));
        q.addQuestion(question("Combien vaut π (Pi) à deux décimales ?", "3,14", "3,12", "3,16", "3,41"));
        q.addQuestion(question("Quel organe produit l'insuline ?", "Le pancréas", "Le foie", "Le rein", "L'estomac"));
        q.addQuestion(question("Quelle est la particule de charge négative dans un atome ?", "L'électron", "Le proton", "Le neutron", "Le photon"));
        q.addQuestion(question("À quelle température l'eau bout-elle au niveau de la mer ?", "100 °C", "90 °C", "110 °C", "80 °C"));
        q.addQuestion(questionLibre("Quel est le symbole chimique du sodium ?", "Na"));
        return q;
    }

    private Quiz buildHistoire() {
        Quiz q = quiz("Histoire mondiale", "Événements et personnages qui ont marqué l'humanité");
        q.addQuestion(question("En quelle année a eu lieu la Révolution française ?", "1789", "1776", "1804", "1815"));
        q.addQuestion(question("Qui était le premier président des États-Unis ?", "George Washington", "Abraham Lincoln", "Thomas Jefferson", "Benjamin Franklin"));
        q.addQuestion(question("Quelle guerre s'est terminée en 1945 ?", "La Seconde Guerre mondiale", "La Première Guerre mondiale", "La guerre de Corée", "La guerre du Vietnam"));
        q.addQuestion(question("En quelle année a eu lieu la chute du mur de Berlin ?", "1989", "1991", "1985", "1979"));
        q.addQuestion(question("Qui a découvert l'Amérique en 1492 ?", "Christophe Colomb", "Amerigo Vespucci", "Vasco de Gama", "Ferdinand Magellan"));
        q.addQuestion(question("En quelle année la France a-t-elle aboli l'esclavage définitivement ?", "1848", "1789", "1815", "1830"));
        q.addQuestion(question("Quelle est la date du débarquement allié en Normandie ?", "6 juin 1944", "6 juin 1945", "8 mai 1945", "11 novembre 1918"));
        q.addQuestion(question("Quel traité a mis fin à la Première Guerre mondiale ?", "Le traité de Versailles", "Le traité de Paris", "Le traité de Berlin", "L'armistice de Vienne"));
        q.addQuestion(question("En quelle année Napoléon a-t-il été exilé à Sainte-Hélène ?", "1815", "1814", "1812", "1821"));
        q.addQuestion(question("Quel peuple a bâti le Colisée de Rome ?", "Les Romains", "Les Grecs", "Les Étrusques", "Les Byzantins"));
        q.addQuestion(questionLibre("En quelle année a eu lieu la chute de Constantinople ?", "1453"));
        return q;
    }

    private Quiz buildSport() {
        Quiz q = quiz("Sport", "Football, tennis, JO et bien plus encore");
        q.addQuestion(question("Combien de joueurs composent une équipe de football ?", "11", "9", "10", "12"));
        q.addQuestion(question("Dans quelle ville ont eu lieu les JO d'été de 2024 ?", "Paris", "Tokyo", "Los Angeles", "Brisbane"));
        q.addQuestion(question("Quel pays a remporté la Coupe du Monde de football en 2018 ?", "France", "Croatie", "Brésil", "Argentine"));
        q.addQuestion(question("Quel sport se joue avec une raquette et un volant ?", "Le badminton", "Le squash", "Le tennis de table", "Le padel"));
        q.addQuestion(question("Quelle distance parcourt-on lors d'un marathon ?", "42,195 km", "40 km", "45 km", "50 km"));
        q.addQuestion(question("Dans quel sport utilise-t-on un palet ?", "Le hockey sur glace", "Le curling", "Le cricket", "Le baseball"));
        q.addQuestion(question("Combien de points vaut un essai en rugby à XV ?", "5", "3", "4", "6"));
        q.addQuestion(question("Quel nageur détient le record du plus grand nombre de médailles olympiques ?", "Michael Phelps", "Mark Spitz", "Ian Thorpe", "Ryan Lochte"));
        q.addQuestion(question("Quelle surface est utilisée à Roland-Garros ?", "La terre battue", "Le gazon", "La surface dure", "L'herbe synthétique"));
        q.addQuestion(question("En quelle année la France a-t-elle gagné la Coupe du Monde de football ?", "1998 et 2018", "1998 seulement", "2018 seulement", "2006 et 2014"));
        q.addQuestion(questionLibre("Combien de joueurs composent une équipe de basketball ?", "5"));
        return q;
    }

    private Quiz buildCinema() {
        Quiz q = quiz("Cinéma", "Films cultes, réalisateurs et palmarès");
        q.addQuestion(question("Qui a réalisé le film Titanic (1997) ?", "James Cameron", "Steven Spielberg", "Christopher Nolan", "Ridley Scott"));
        q.addQuestion(question("Dans quel film la réplique « Je reviendrai » est-elle célèbre ?", "Terminator", "Predator", "RoboCop", "Total Recall"));
        q.addQuestion(question("Qui joue le rôle de Jack dans Titanic ?", "Leonardo DiCaprio", "Brad Pitt", "Johnny Depp", "Tom Hanks"));
        q.addQuestion(question("Quelle franchise a lancé l'univers cinématographique Marvel en 2008 ?", "Iron Man", "Thor", "Captain America", "Hulk"));
        q.addQuestion(question("Dans quel pays le Festival de Cannes a-t-il lieu chaque année ?", "France", "Italie", "Espagne", "Belgique"));
        q.addQuestion(question("Qui a réalisé la trilogie Le Seigneur des anneaux ?", "Peter Jackson", "Steven Spielberg", "George Lucas", "Ridley Scott"));
        q.addQuestion(question("Quel film d'animation Pixar met en scène un rat cuisinier ?", "Ratatouille", "Coco", "Brave", "Soul"));
        q.addQuestion(question("Qui joue Hermione Granger dans Harry Potter ?", "Emma Watson", "Emma Stone", "Keira Knightley", "Natalie Portman"));
        q.addQuestion(question("Combien d'Oscars a remporté Le Retour du Roi (2003) ?", "11", "9", "7", "13"));
        q.addQuestion(questionLibre("Quel acteur joue le rôle de Tony Stark / Iron Man ?", "Robert Downey Jr"));
        return q;
    }

    private Quiz buildMusique() {
        Quiz q = quiz("Musique", "Artistes, genres musicaux et histoire de la musique");
        q.addQuestion(question("Quel groupe britannique comprenait John Lennon et Paul McCartney ?", "Les Beatles", "Les Rolling Stones", "Queen", "Pink Floyd"));
        q.addQuestion(question("Qui est surnommé le « Roi du rock and roll » ?", "Elvis Presley", "Chuck Berry", "Jerry Lee Lewis", "Little Richard"));
        q.addQuestion(question("Quel instrument Beethoven jouait-il principalement ?", "Le piano", "Le violon", "La flûte", "L'orgue"));
        q.addQuestion(question("De quelle nationalité était Mozart ?", "Autrichienne", "Allemande", "Italienne", "Tchèque"));
        q.addQuestion(question("Quel genre musical est originaire de la Jamaïque ?", "Le reggae", "Le ska", "Le dancehall", "Le calypso"));
        q.addQuestion(question("Combien de cordes a une guitare standard ?", "6", "4", "7", "5"));
        q.addQuestion(question("Quel chanteur français est connu pour Ne me quitte pas ?", "Jacques Brel", "Charles Aznavour", "Georges Brassens", "Serge Gainsbourg"));
        q.addQuestion(question("Quel artiste a réalisé l'album Thriller, le plus vendu de l'histoire ?", "Michael Jackson", "Prince", "Madonna", "Whitney Houston"));
        q.addQuestion(question("Quel groupe de rock est connu pour Bohemian Rhapsody ?", "Queen", "Led Zeppelin", "AC/DC", "The Who"));
        q.addQuestion(questionLibre("Comment s'appelle le chanteur de Queen ?", "Freddie Mercury"));
        return q;
    }

    private Quiz buildGastronomie() {
        Quiz q = quiz("Gastronomie", "Cuisine française et mondiale, ingrédients et recettes");
        q.addQuestion(question("Quelle ville française est la capitale mondiale de la gastronomie ?", "Lyon", "Paris", "Bordeaux", "Toulouse"));
        q.addQuestion(question("De quel pays est originaire la pizza Margherita ?", "Italie", "France", "Grèce", "Espagne"));
        q.addQuestion(question("Quel est le principal ingrédient de la bouillabaisse ?", "Le poisson", "Le poulet", "Le mouton", "Le bœuf"));
        q.addQuestion(question("Dans quel pays mange-t-on du kimchi traditionnellement ?", "Corée", "Japon", "Chine", "Vietnam"));
        q.addQuestion(question("Quel épice colore le riz en jaune dans la paella ?", "Le safran", "Le curcuma", "Le cumin", "Le paprika"));
        q.addQuestion(question("Qu'est-ce que le foie gras ?", "Un foie d'oie ou de canard gavé", "Un pâté de porc", "Un fromage normand", "Un saucisson lyonnais"));
        q.addQuestion(question("Quel fromage suisse a des trous caractéristiques ?", "L'Emmental", "Le Gruyère", "L'Appenzell", "Le Raclette"));
        q.addQuestion(question("Quelle pâtisserie française est composée de deux coques de meringue colorées ?", "Le macaron", "L'éclair", "Le mille-feuille", "Le profiterole"));
        q.addQuestion(question("Quel alcool entre dans la composition du cocktail Mojito ?", "Le rhum blanc", "La vodka", "La tequila", "Le gin"));
        q.addQuestion(questionLibre("Quelle ville est connue pour son cassoulet ?", "Castelnaudary"));
        return q;
    }

    private Quiz buildAnimaux() {
        Quiz q = quiz("Le règne animal", "Faune sauvage, comportements et records animaux");
        q.addQuestion(question("Quel animal est le plus rapide du monde ?", "Le guépard", "Le lion", "L'aigle", "Le cheval"));
        q.addQuestion(question("Combien de cœurs possède un poulpe ?", "3", "1", "2", "4"));
        q.addQuestion(question("Quel est le plus grand animal terrestre ?", "L'éléphant d'Afrique", "La girafe", "L'hippopotame", "Le rhinocéros"));
        q.addQuestion(question("Combien de pattes a une araignée ?", "8", "6", "10", "12"));
        q.addQuestion(question("Quelle espèce de requin est la plus grande ?", "Le requin baleine", "Le grand requin blanc", "Le requin marteau", "Le requin taureau"));
        q.addQuestion(question("Comment appelle-t-on le petit du cheval ?", "Le poulain", "Le veau", "Le chevreau", "Le faon"));
        q.addQuestion(question("Quel oiseau peut imiter la voix humaine ?", "Le perroquet", "Le corbeau", "Le moineau", "Le canari"));
        q.addQuestion(question("Quelle est la durée de gestation de l'éléphant ?", "22 mois", "9 mois", "12 mois", "18 mois"));
        q.addQuestion(question("Quel insecte produit le miel ?", "L'abeille", "La guêpe", "Le bourdon", "Le frelon"));
        q.addQuestion(questionLibre("Combien de pattes a un insecte ?", "6"));
        return q;
    }

    private Quiz buildMythologie() {
        Quiz q = quiz("Mythologie", "Dieux, héros et légendes de l'Antiquité");
        q.addQuestion(question("Qui est le dieu principal du panthéon grec ?", "Zeus", "Poséidon", "Hadès", "Apollon"));
        q.addQuestion(question("Quel héros grec a accompli les douze travaux ?", "Héraclès", "Achille", "Ulysse", "Thésée"));
        q.addQuestion(question("Qui est l'équivalent romain de Zeus ?", "Jupiter", "Mars", "Neptune", "Mercure"));
        q.addQuestion(question("Quel monstre gardait le labyrinthe en Crète ?", "Le Minotaure", "La Méduse", "Le Sphinx", "La Chimère"));
        q.addQuestion(question("Quelle déesse est associée à la sagesse dans la mythologie grecque ?", "Athéna", "Artémis", "Héra", "Aphrodite"));
        q.addQuestion(question("Qui est le dieu nordique associé au tonnerre ?", "Thor", "Odin", "Loki", "Freyr"));
        q.addQuestion(question("Quel fleuve les âmes traversaient-elles pour entrer aux Enfers ?", "Le Styx", "L'Achéron", "Le Léthé", "Le Phlégéthon"));
        q.addQuestion(question("Qui a tué Méduse ?", "Persée", "Thésée", "Achille", "Jason"));
        q.addQuestion(question("Qui est la déesse de l'amour dans la mythologie romaine ?", "Vénus", "Junon", "Diane", "Minerve"));
        q.addQuestion(questionLibre("Comment s'appelle le cheval à ailes de la mythologie grecque ?", "Pégase"));
        return q;
    }

    private Quiz buildTechno() {
        Quiz q = quiz("Technologie et informatique", "Internet, programmation et innovations numériques");
        q.addQuestion(question("Qui a fondé Microsoft ?", "Bill Gates et Paul Allen", "Steve Jobs", "Mark Zuckerberg", "Jeff Bezos"));
        q.addQuestion(question("En quelle année le premier iPhone a-t-il été lancé ?", "2007", "2005", "2009", "2010"));
        q.addQuestion(question("Que signifie l'acronyme HTML ?", "HyperText Markup Language", "HyperText Making Language", "High Text Machine Language", "HyperTransfer Mode Language"));
        q.addQuestion(question("Quel langage de programmation est surnommé « la langue de la toile » ?", "JavaScript", "Python", "Java", "PHP"));
        q.addQuestion(question("Que signifie le sigle CPU ?", "Central Processing Unit", "Computer Power Unit", "Core Processing Unit", "Central Program Utility"));
        q.addQuestion(question("Qui est le créateur de Linux ?", "Linus Torvalds", "Dennis Ritchie", "Ken Thompson", "Richard Stallman"));
        q.addQuestion(question("Quel réseau social a été fondé par Mark Zuckerberg ?", "Facebook", "Twitter", "Instagram", "Snapchat"));
        q.addQuestion(question("Quelle technologie permet la connexion sans fil à courte portée ?", "Bluetooth", "Wi-Fi", "NFC", "4G"));
        q.addQuestion(question("Quel format d'image est sans perte de qualité ?", "PNG", "JPEG", "GIF", "BMP"));
        q.addQuestion(questionLibre("Quel langage de programmation a été créé par Guido van Rossum ?", "Python"));
        return q;
    }

    private Quiz buildLitterature() {
        Quiz q = quiz("Littérature", "Romans, poètes et auteurs célèbres");
        q.addQuestion(question("Qui a écrit Les Misérables ?", "Victor Hugo", "Émile Zola", "Gustave Flaubert", "Alexandre Dumas"));
        q.addQuestion(question("Qui a écrit L'Étranger ?", "Albert Camus", "Jean-Paul Sartre", "André Gide", "Simone de Beauvoir"));
        q.addQuestion(question("Quel auteur a créé le personnage de Don Quichotte ?", "Miguel de Cervantes", "Lope de Vega", "Federico García Lorca", "Calderón de la Barca"));
        q.addQuestion(question("Dans quel livre apparaît le chapelier fou ?", "Alice au pays des merveilles", "Peter Pan", "Le Magicien d'Oz", "Les Voyages de Gulliver"));
        q.addQuestion(question("Qui a écrit Madame Bovary ?", "Gustave Flaubert", "Victor Hugo", "Honoré de Balzac", "Stendhal"));
        q.addQuestion(question("Quel poète français a écrit Les Fleurs du mal ?", "Charles Baudelaire", "Arthur Rimbaud", "Paul Verlaine", "Stéphane Mallarmé"));
        q.addQuestion(question("Qui a écrit la saga Harry Potter ?", "J.K. Rowling", "Tolkien", "C.S. Lewis", "Philip Pullman"));
        q.addQuestion(question("Quel auteur russe a écrit Crime et Châtiment ?", "Fiodor Dostoïevski", "Léon Tolstoï", "Anton Tchekhov", "Ivan Tourgueniev"));
        q.addQuestion(question("Qui a écrit L'Iliade et L'Odyssée ?", "Homère", "Virgile", "Sophocle", "Euripide"));
        q.addQuestion(questionLibre("Quel roman de Victor Hugo met en scène Quasimodo ?", "Notre-Dame de Paris"));
        return q;
    }

    private Quiz buildAstronomie() {
        Quiz q = quiz("Astronomie", "Planètes, étoiles et mystères de l'univers");
        q.addQuestion(question("Combien de planètes compte notre système solaire ?", "8", "9", "7", "10"));
        q.addQuestion(question("Quelle est l'étoile la plus proche de la Terre (hors Soleil) ?", "Proxima du Centaure", "Sirius", "Bételgeuse", "Vega"));
        q.addQuestion(question("Quel est le plus grand satellite naturel de Saturne ?", "Titan", "Europe", "Ganymède", "Io"));
        q.addQuestion(question("Quelle planète possède la plus grande tempête connue du système solaire ?", "Jupiter", "Saturne", "Neptune", "Uranus"));
        q.addQuestion(question("En quelle année Youri Gagarine est-il devenu le premier homme dans l'espace ?", "1961", "1957", "1965", "1969"));
        q.addQuestion(question("Combien de temps la lumière met-elle environ pour aller du Soleil à la Terre ?", "8 minutes", "1 heure", "1 seconde", "24 heures"));
        q.addQuestion(question("Quelle planète est la plus éloignée du Soleil ?", "Neptune", "Uranus", "Saturne", "Pluton"));
        q.addQuestion(question("Comment appelle-t-on une étoile en fin de vie qui explose ?", "Une supernova", "Une nébuleuse", "Un pulsar", "Un quasar"));
        q.addQuestion(question("Quelle est la composition principale du Soleil ?", "Hydrogène et hélium", "Oxygène et azote", "Carbone et silicium", "Fer et nickel"));
        q.addQuestion(question("En quelle année l'homme a-t-il marché sur la Lune pour la première fois ?", "1969", "1967", "1971", "1965"));
        q.addQuestion(questionLibre("Quelle est la planète la plus proche du Soleil ?", "Mercure"));
        return q;
    }
}
