import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import QuizCard from "../components/quiz/QuizCard";
import { getQuizzes } from "../services/quizService";
import type { Quiz } from "../types/quiz";

export default function Quizzes() {
    const [quizzes, setQuizzes] = useState<Quiz[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const loadQuizzes = async () => {
            try {
                const data = await getQuizzes();

                console.log("Réponse API quiz :", data);

                setQuizzes(data);
            } catch (error) {
                console.error(error);
                setError("Impossible de charger les quiz.");
            } finally {
                setLoading(false);
            }
        };

        loadQuizzes();
    }, []);

    if (loading) {
        return <p>Chargement des quiz...</p>;
    }

    if (error) {
        return <p>{error}</p>;
    }

    return (
        <section>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12 }}>
                <h1>Les quiz</h1>
                <Link to="/quizzes/create"><button>✏️ Créer un quiz</button></Link>
            </div>

            {quizzes.length === 0 ? (
                <p>Aucun quiz disponible.</p>
            ) : (
                <div className="quiz-grid">
                    {quizzes.map((quiz) => (
                        <QuizCard
                            key={quiz.id}
                            quiz={quiz}
                        />
                    ))}
                </div>
            )}
        </section>
    );
}