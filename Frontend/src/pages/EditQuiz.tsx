import {useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import type {Quiz} from "../types/quiz";
import {
    getQuizById,
    deleteQuiz,
} from "../services/quizService";
import api from "../services/api";

export default function EditQuiz() {
    const {quizId} = useParams<{ quizId: string }>();
    const navigate = useNavigate();

    const [quiz, setQuiz] = useState<Quiz | null>(null);
    const [titre, setTitre] = useState("");
    const [description, setDescription] = useState("");

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!quizId) return;

        const loadQuiz = async () => {
            try {
                const data = await getQuizById(Number(quizId));

                setQuiz(data);
                setTitre(data.titre);
                setDescription(data.description);
            } catch (error) {
                console.error(error);
                setError("Impossible de charger le quiz.");
            } finally {
                setLoading(false);
            }
        };

        loadQuiz();
    }, [quizId]);

    const saveQuiz = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!quizId || !quiz) return;

        if (!titre.trim()) {
            setError("Le titre est requis.");
            return;
        }

        setSaving(true);
        setError("");

        try {
            await api.put(`/quizs/${quizId}/`, {
                id: Number(quizId),
                titre: titre.trim(),
                description: description.trim(),
            });

            navigate(`/quizzes/${quizId}`);
        } catch (error) {
            console.error(error);
            setError("Impossible de modifier le quiz.");
        } finally {
            setSaving(false);
        }
    };

    const removeQuestion = async (questionId: number) => {
        if (!quizId) return;

        const confirmed = window.confirm(
            "Supprimer cette question du quiz ?"
        );

        if (!confirmed) return;

        try {
            const response = await api.delete(
                `/quizs/${quizId}/questions/${questionId}/`
            );

            setQuiz(response.data);
        } catch (error) {
            console.error(error);
            setError("Impossible de supprimer la question.");
        }
    };

    const removeQuiz = async () => {
        if (!quizId) return;

        const confirmed = window.confirm(
            "Êtes-vous sûr de vouloir supprimer définitivement ce quiz ?"
        );

        if (!confirmed) return;

        try {
            await deleteQuiz(Number(quizId));
            navigate("/quizzes");
        } catch (error) {
            console.error(error);
            setError("Impossible de supprimer le quiz.");
        }
    };

    if (loading) {
        return <p>Chargement du quiz...</p>;
    }

    if (!quiz) {
        return <p>{error || "Quiz introuvable."}</p>;
    }

    return (
        <div className="create-quiz">
            <h1>Modifier le quiz</h1>

            <form onSubmit={saveQuiz}>
                <label>
                    Titre

                    <input
                        value={titre}
                        onChange={(e) => setTitre(e.target.value)}
                        placeholder="Ex : Culture générale"
                        required
                    />
                </label>

                <label>
                    Description

                    <input
                        value={description}
                        onChange={(e) =>
                            setDescription(e.target.value)
                        }
                    />
                </label>

                <h2>
                    Questions ({quiz.questions.length})
                </h2>

                {quiz.questions.map((question, index) => (
                    <div
                        key={question.id}
                        className="question-block"
                    >
                        <h3>
                            Question {index + 1}
                        </h3>

                        <p>
                            {question.text}
                        </p>

                        <p>
                            Type : {question.type}
                        </p>

                        {question.type === "CHOIX" && (
                            <div>
                                {question.reponses.map(
                                    (reponse) => (
                                        <div
                                            key={reponse.id}
                                            className="answer-row"
                                        >
                                            <span>
                                                {reponse.text}
                                            </span>

                                            {reponse.estBonne && (
                                                <span> ✅</span>
                                            )}
                                        </div>
                                    )
                                )}
                            </div>
                        )}

                        {question.type === "SAISIE_LIBRE" && (
                            <p>
                                Bonne réponse :{" "}
                                {
                                    question.reponses[0]?.text
                                }
                            </p>
                        )}

                        <button
                            type="button"
                            className="remove-q"
                            onClick={() =>
                                removeQuestion(question.id)
                            }
                        >
                            🗑️ Supprimer la question
                        </button>
                    </div>
                ))}

                {error && (
                    <p className="error">
                        {error}
                    </p>
                )}

                <button
                    type="submit"
                    disabled={saving}
                >
                    {saving
                        ? "Enregistrement..."
                        : "💾 Enregistrer"}
                </button>
            </form>

            <hr/>

            <button
                type="button"
                onClick={removeQuiz}
            >
                🗑️ Supprimer définitivement le quiz
            </button>

            <button
                type="button"
                onClick={() =>
                    navigate(`/quizzes/${quizId}`)
                }
            >
                ← Annuler
            </button>
        </div>
    );
}