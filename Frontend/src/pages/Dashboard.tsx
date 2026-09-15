import { useNavigate } from "react-router-dom";
import Card from "../components/common/Card";

export default function Dashboard() {
    const navigate = useNavigate();

    return (
        <section>
            <h1>Bienvenue sur Game Arena 👋</h1>

            <p>
                Prépare-toi à tester tes connaissances et affronter
                d'autres joueurs.
            </p>

            <div className="dashboard-cards">

                <Card
                    title="🎯 Les quiz"
                    description="Découvre les quiz disponibles et teste tes connaissances."
                    buttonText="Voir les quiz"
                    onClick={() => navigate("/quizzes")}
                />

                <Card
                    title="👥 Rejoindre un salon"
                    description="Rejoins une partie grâce au code de ton salon."
                    buttonText="Rejoindre"
                    onClick={() => navigate("/salon/join")}
                />

                <Card
                    title="🎮 Créer un salon"
                    description="Crée une partie et invite tes amis."
                    buttonText="Créer un salon"
                    onClick={() => navigate("/salon")}
                />

                <Card
                    title="✏️ Créer un quiz"
                    description="Compose tes propres questions et défie tes amis."
                    buttonText="Créer"
                    onClick={() => navigate("/quizzes/create")}
                />

            </div>
        </section>
    );
}