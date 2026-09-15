import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { Question } from "../types/quiz";
import {
    getQuizById,
    deleteQuiz,
    addQuestionToQuiz,
    updateQuestionInQuiz,
    removeQuestionFromQuiz,
    updateQuizMeta,
    type QuestionForm,
    type ReponseForm,
} from "../services/quizService";

type QuestionType = "CHOIX" | "SAISIE_LIBRE";

const emptyChoixReponses = (): ReponseForm[] => [
    { text: "", estBonne: true },
    { text: "", estBonne: false },
    { text: "", estBonne: false },
    { text: "", estBonne: false },
];

const emptyForm = (type: QuestionType = "CHOIX"): QuestionForm => ({
    text: "",
    type,
    reponses: type === "SAISIE_LIBRE" ? [{ text: "", estBonne: true }] : emptyChoixReponses(),
});

// Convertit une Question existante en formulaire éditable
const questionToForm = (q: Question): QuestionForm => ({
    text: q.text,
    type: q.type as QuestionType,
    reponses: q.reponses.map(r => ({ text: r.text, estBonne: r.estBonne })),
});

export default function QuizDetails() {
    const { quizId } = useParams<{ quizId: string }>();
    const navigate = useNavigate();

    // ── État principal ──
    const [quiz, setQuiz] = useState<Awaited<ReturnType<typeof getQuizById>> | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // ── Mode édition titre/desc ──
    const [editingMeta, setEditingMeta] = useState(false);
    const [metaTitre, setMetaTitre] = useState("");
    const [metaDesc, setMetaDesc] = useState("");
    const [savingMeta, setSavingMeta] = useState(false);

    // ── Ajout d'une nouvelle question ──
    const [showAddForm, setShowAddForm] = useState(false);
    const [newQuestion, setNewQuestion] = useState<QuestionForm>(emptyForm());
    const [addingQuestion, setAddingQuestion] = useState(false);

    // ── Édition d'une question existante ──
    const [editingQuestionId, setEditingQuestionId] = useState<number | null>(null);
    const [editForm, setEditForm] = useState<QuestionForm>(emptyForm());
    const [savingQuestion, setSavingQuestion] = useState(false);

    // ── Chargement ──
    const load = async () => {
        if (!quizId) return;
        try {
            const data = await getQuizById(Number(quizId));
            setQuiz(data);
            setMetaTitre(data.titre);
            setMetaDesc(data.description);
        } catch {
            setError("Impossible de charger le quiz.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, [quizId]);

    // ── Suppression du quiz ──
    const handleDeleteQuiz = async () => {
        if (!quizId || !window.confirm("Supprimer ce quiz et toutes ses questions ?")) return;
        try {
            await deleteQuiz(Number(quizId));
            navigate("/quizzes");
        } catch {
            setError("Impossible de supprimer le quiz.");
        }
    };

    // ── Sauvegarde titre/desc ──
    const handleSaveMeta = async () => {
        if (!quizId || !metaTitre.trim()) return;
        setSavingMeta(true);
        try {
            const updated = await updateQuizMeta(Number(quizId), metaTitre.trim(), metaDesc.trim());
            setQuiz(updated);
            setEditingMeta(false);
        } catch (err: any) {
            if (err?.response?.status === 409) {
                setError("⚠️ " + (err.response?.data || "Ce quiz a été modifié par quelqu'un d'autre. Rechargez la page."));
                // Recharge automatiquement pour avoir la version à jour
                await load();
            } else {
                setError("Impossible de modifier le quiz.");
            }
        } finally {
            setSavingMeta(false);
        }
    };

    // ── Helpers formulaire question ──
    const setReponseText = (form: QuestionForm, ri: number, text: string): QuestionForm => ({
        ...form,
        reponses: form.reponses.map((r, i) => i === ri ? { ...r, text } : r),
    });

    const setCorrect = (form: QuestionForm, ri: number): QuestionForm => ({
        ...form,
        reponses: form.reponses.map((r, i) => ({ ...r, estBonne: i === ri })),
    });

    const setType = (form: QuestionForm, type: QuestionType): QuestionForm => ({
        ...form,
        type,
        reponses: type === "SAISIE_LIBRE" ? [{ text: "", estBonne: true }] : emptyChoixReponses(),
    });

    const validateForm = (form: QuestionForm): string | null => {
        if (!form.text.trim()) return "Le texte de la question est requis.";
        const filled = form.reponses.filter(r => r.text.trim());
        if (form.type === "CHOIX" && filled.length < 2) return "Remplis au moins 2 réponses.";
        if (form.type === "SAISIE_LIBRE" && !filled[0]?.text.trim()) return "Remplis la bonne réponse.";
        if (!filled.some(r => r.estBonne)) return "Coche la bonne réponse.";
        return null;
    };

    // ── Ajout question ──
    const handleAddQuestion = async () => {
        const err = validateForm(newQuestion);
        if (err) { setError(err); return; }
        if (!quizId) return;
        setAddingQuestion(true);
        setError("");
        try {
            const updated = await addQuestionToQuiz(Number(quizId), {
                ...newQuestion,
                reponses: newQuestion.reponses.filter(r => r.text.trim()),
            });
            setQuiz(updated);
            setNewQuestion(emptyForm());
            setShowAddForm(false);
        } catch (err: any) {
            if (err?.response?.status === 409) {
                setError("⚠️ Le quiz a été modifié par quelqu'un d'autre. Rechargez la page.");
                await load();
            } else {
                setError("Impossible d'ajouter la question.");
            }
        } finally {
            setAddingQuestion(false);
        }
    };

    // ── Édition question ──
    const startEdit = (q: Question) => {
        setEditingQuestionId(q.id);
        setEditForm(questionToForm(q));
        setError("");
    };

    const handleSaveQuestion = async () => {
        const err = validateForm(editForm);
        if (err) { setError(err); return; }
        if (!quizId || editingQuestionId === null) return;
        setSavingQuestion(true);
        setError("");
        try {
            const updated = await updateQuestionInQuiz(Number(quizId), editingQuestionId, {
                ...editForm,
                reponses: editForm.reponses.filter(r => r.text.trim()),
            });
            setQuiz(updated);
            setEditingQuestionId(null);
        } catch (err: any) {
            if (err?.response?.status === 409) {
                setError("⚠️ Le quiz a été modifié par quelqu'un d'autre. Rechargez la page.");
                await load();
            } else {
                setError("Impossible de modifier la question.");
            }
        } finally {
            setSavingQuestion(false);
        }
    };

    // ── Suppression question ──
    const handleDeleteQuestion = async (questionId: number) => {
        if (!quizId || !window.confirm("Supprimer cette question ?")) return;
        try {
            const updated = await removeQuestionFromQuiz(Number(quizId), questionId);
            setQuiz(updated);
        } catch (err: any) {
            if (err?.response?.status === 409) {
                setError("⚠️ Le quiz a été modifié par quelqu'un d'autre. Rechargez la page.");
                await load();
            } else {
                setError("Impossible de supprimer la question.");
            }
        }
    };

    if (loading) return <p>Chargement...</p>;
    if (!quiz) return <p className="error">{error || "Quiz introuvable."}</p>;

    return (
        <div className="create-quiz">

            {/* ── En-tête quiz ── */}
            {editingMeta ? (
                <div className="question-block">
                    <h2>Modifier le quiz</h2>
                    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                        <div>
                            <label>Titre</label>
                            <input value={metaTitre} onChange={e => setMetaTitre(e.target.value)} />
                        </div>
                        <div>
                            <label>Description</label>
                            <input value={metaDesc} onChange={e => setMetaDesc(e.target.value)} />
                        </div>
                        <div style={{ display: "flex", gap: 8 }}>
                            <button onClick={handleSaveMeta} disabled={savingMeta}>
                                {savingMeta ? "Enregistrement..." : "💾 Enregistrer"}
                            </button>
                            <button className="btn-outline" onClick={() => setEditingMeta(false)}>Annuler</button>
                        </div>
                    </div>
                </div>
            ) : (
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 12, marginBottom: 8 }}>
                    <div>
                        <h1>{quiz.titre}</h1>
                        <p style={{ margin: 0, opacity: .7 }}>{quiz.description}</p>
                    </div>
                    <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                        <button className="btn-outline" onClick={() => setEditingMeta(true)}>✏️ Modifier</button>
                        <button className="btn-danger" onClick={handleDeleteQuiz}>🗑️ Supprimer</button>
                    </div>
                </div>
            )}

            {error && <p className="error">{error}</p>}

            {/* ── Liste questions ── */}
            <h2 style={{ marginTop: 24 }}>Questions ({quiz.questions.length})</h2>

            {quiz.questions.map((question, index) => (
                <div key={question.id} className="question-block">

                    {editingQuestionId === question.id ? (
                        /* ── Formulaire édition ── */
                        <>
                            <h3>Modifier la question {index + 1}</h3>

                            <div style={{ marginBottom: 10 }}>
                                <label>Type</label>
                                <select
                                    value={editForm.type}
                                    onChange={e => setEditForm(f => setType(f, e.target.value as QuestionType))}>
                                    <option value="CHOIX">Choix multiple</option>
                                    <option value="SAISIE_LIBRE">Saisie libre</option>
                                </select>
                            </div>

                            <div style={{ marginBottom: 12 }}>
                                <label>Question</label>
                                <input
                                    value={editForm.text}
                                    onChange={e => setEditForm(f => ({ ...f, text: e.target.value }))}
                                    placeholder="Texte de la question" />
                            </div>

                            {editForm.type === "CHOIX" ? (
                                editForm.reponses.map((r, ri) => (
                                    <div key={ri} className="answer-row">
                                        <input
                                            value={r.text}
                                            onChange={e => setEditForm(f => setReponseText(f, ri, e.target.value))}
                                            placeholder={`Réponse ${ri + 1}`} />
                                        <label>
                                            <input type="radio" name={`edit-correct-${question.id}`}
                                                checked={r.estBonne}
                                                onChange={() => setEditForm(f => setCorrect(f, ri))} />
                                            ✓
                                        </label>
                                    </div>
                                ))
                            ) : (
                                <div>
                                    <label>Bonne réponse</label>
                                    <input
                                        value={editForm.reponses[0]?.text ?? ""}
                                        onChange={e => setEditForm(f => setReponseText(f, 0, e.target.value))}
                                        placeholder="Ex : Paris" />
                                </div>
                            )}

                            <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                                <button onClick={handleSaveQuestion} disabled={savingQuestion}>
                                    {savingQuestion ? "Enregistrement..." : "💾 Sauvegarder"}
                                </button>
                                <button className="btn-outline" onClick={() => setEditingQuestionId(null)}>Annuler</button>
                            </div>
                        </>
                    ) : (
                        /* ── Affichage normal ── */
                        <>
                            <button className="remove-q" onClick={() => handleDeleteQuestion(question.id)} title="Supprimer">🗑️</button>

                            <h3>Question {index + 1}
                                <span style={{ fontSize: 12, fontWeight: 400, marginLeft: 8, opacity: .6 }}>
                                    {question.type === "SAISIE_LIBRE" ? "Saisie libre" : "Choix multiple"}
                                </span>
                            </h3>

                            <p style={{ fontWeight: 600, color: "var(--text-h)" }}>{question.text}</p>

                            {question.type === "CHOIX" ? (
                                <ul style={{ margin: "8px 0 0", paddingLeft: 20 }}>
                                    {question.reponses.map(r => (
                                        <li key={r.id} style={{ color: r.estBonne ? "var(--green)" : undefined }}>
                                            {r.text} {r.estBonne && "✅"}
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p style={{ color: "var(--green)" }}>
                                    Bonne réponse : <strong>{question.reponses[0]?.text}</strong>
                                </p>
                            )}

                            <button
                                className="btn-outline"
                                style={{ marginTop: 10, padding: "6px 14px", fontSize: 13 }}
                                onClick={() => startEdit(question)}>
                                ✏️ Modifier
                            </button>
                        </>
                    )}
                </div>
            ))}

            {/* ── Ajout question ── */}
            {showAddForm ? (
                <div className="question-block">
                    <h3>Nouvelle question</h3>

                    <div style={{ marginBottom: 10 }}>
                        <label>Type</label>
                        <select
                            value={newQuestion.type}
                            onChange={e => setNewQuestion(f => setType(f, e.target.value as QuestionType))}>
                            <option value="CHOIX">Choix multiple</option>
                            <option value="SAISIE_LIBRE">Saisie libre</option>
                        </select>
                    </div>

                    <div style={{ marginBottom: 12 }}>
                        <label>Question</label>
                        <input
                            value={newQuestion.text}
                            onChange={e => setNewQuestion(f => ({ ...f, text: e.target.value }))}
                            placeholder="Texte de la question"
                            autoFocus />
                    </div>

                    {newQuestion.type === "CHOIX" ? (
                        newQuestion.reponses.map((r, ri) => (
                            <div key={ri} className="answer-row">
                                <input
                                    value={r.text}
                                    onChange={e => setNewQuestion(f => setReponseText(f, ri, e.target.value))}
                                    placeholder={`Réponse ${ri + 1}`} />
                                <label>
                                    <input type="radio" name="new-correct"
                                        checked={r.estBonne}
                                        onChange={() => setNewQuestion(f => setCorrect(f, ri))} />
                                    ✓
                                </label>
                            </div>
                        ))
                    ) : (
                        <div>
                            <label>Bonne réponse</label>
                            <input
                                value={newQuestion.reponses[0]?.text ?? ""}
                                onChange={e => setNewQuestion(f => setReponseText(f, 0, e.target.value))}
                                placeholder="Ex : Paris" />
                        </div>
                    )}

                    <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                        <button onClick={handleAddQuestion} disabled={addingQuestion}>
                            {addingQuestion ? "Ajout..." : "➕ Ajouter"}
                        </button>
                        <button className="btn-outline" onClick={() => { setShowAddForm(false); setNewQuestion(emptyForm()); setError(""); }}>
                            Annuler
                        </button>
                    </div>
                </div>
            ) : (
                <button className="add-question-btn" onClick={() => setShowAddForm(true)}>
                    ➕ Ajouter une question
                </button>
            )}

            <div style={{ marginTop: 24, display: "flex", gap: 10 }}>
                <button className="btn-outline" onClick={() => navigate("/quizzes")}>
                    ← Retour aux quiz
                </button>
            </div>
        </div>
    );
}
