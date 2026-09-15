import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

type QuestionType = "CHOIX" | "SAISIE_LIBRE";
interface ReponseForm { text: string; estBonne: boolean; }
interface QuestionForm { text: string; type: QuestionType; reponses: ReponseForm[]; }
const emptyReponses = (): ReponseForm[] => [
    { text: "", estBonne: true }, { text: "", estBonne: false },
    { text: "", estBonne: false }, { text: "", estBonne: false },
];
const emptyFreeResponse = (): ReponseForm[] => [{ text: "", estBonne: true }];

export default function CreateQuiz() {
    const navigate = useNavigate();
    const [titre, setTitre] = useState("");
    const [description, setDescription] = useState("");
    const [questions, setQuestions] = useState<QuestionForm[]>([{ text: "", type: "CHOIX", reponses: emptyReponses() }]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const updateQuestion = (qi: number, patch: Partial<QuestionForm>) => {
        setQuestions((current) => current.map((q, i) => i === qi ? { ...q, ...patch } : q));
    };
    const setType = (qi: number, type: QuestionType) => updateQuestion(qi, { type, reponses: type === "SAISIE_LIBRE" ? emptyFreeResponse() : emptyReponses() });
    const updateReponseText = (qi: number, ri: number, text: string) => setQuestions((current) => current.map((q, i) => i !== qi ? q : { ...q, reponses: q.reponses.map((r, j) => j === ri ? { ...r, text } : r) }));
    const setCorrect = (qi: number, ri: number) => setQuestions((current) => current.map((q, i) => i !== qi ? q : { ...q, reponses: q.reponses.map((r, j) => ({ ...r, estBonne: j === ri })) }));

    const validate = () => {
        if (!titre.trim()) return "Le titre est requis.";
        for (let i = 0; i < questions.length; i++) {
            const q = questions[i];
            if (!q.text.trim()) return `La question ${i + 1} n'a pas de texte.`;
            const filled = q.reponses.filter((r) => r.text.trim());
            if (q.type === "CHOIX" && filled.length < 2) return `La question ${i + 1} doit avoir au moins 2 réponses.`;
            if (!filled.some((r) => r.estBonne)) return `La question ${i + 1} n'a pas de bonne réponse.`;
            if (q.type === "SAISIE_LIBRE" && filled.length !== 1) return `La question ${i + 1} en saisie libre doit avoir une seule bonne réponse.`;
        }
        return null;
    };

    const submit = async (e: React.FormEvent) => {
        e.preventDefault(); setError("");
        const err = validate(); if (err) { setError(err); return; }
        setLoading(true);
        try {
            await api.post("/quizs/full", {
                titre: titre.trim(), description: description.trim(),
                questions: questions.map((q) => ({
                    text: q.text.trim(), type: q.type,
                    reponses: q.reponses.filter((r) => r.text.trim()).map((r) => ({ text: r.text.trim(), estBonne: r.estBonne }))
                }))
            });
            navigate("/quizzes");
        } catch { setError("Impossible de créer le quiz."); } finally { setLoading(false); }
    };

    return (
        <div className="create-quiz">
            <h1>Créer un quiz</h1>
            <form onSubmit={submit}>
                <label>Titre<input value={titre} onChange={(e) => setTitre(e.target.value)} placeholder="Ex : Culture générale" required /></label>
                <label>Description (optionnelle)<input value={description} onChange={(e) => setDescription(e.target.value)} /></label>
                <h2>Questions</h2>
                {questions.map((q, qi) => (
                    <div key={qi} className="question-block">
                        <h3>Question {qi + 1}</h3>
                        <input value={q.text} onChange={(e) => updateQuestion(qi, { text: e.target.value })} placeholder="Texte de la question" />
                        <label>Type de question<select value={q.type} onChange={(e) => setType(qi, e.target.value as QuestionType)}><option value="CHOIX">Choix de réponse</option><option value="SAISIE_LIBRE">Saisie libre (date, chiffre, texte)</option></select></label>
                        {q.type === "SAISIE_LIBRE" ? (
                            <div className="answer-row"><input value={q.reponses[0].text} onChange={(e) => updateReponseText(qi, 0, e.target.value)} placeholder="Bonne réponse (ex : 1789 ou 06/06/1944)" /></div>
                        ) : q.reponses.map((r, ri) => (
                            <div key={ri} className="answer-row"><input value={r.text} onChange={(e) => updateReponseText(qi, ri, e.target.value)} placeholder={`Réponse ${ri + 1}`} /><label><input type="radio" name={`correct-${qi}`} checked={r.estBonne} onChange={() => setCorrect(qi, ri)} /> ✓</label></div>
                        ))}
                        {questions.length > 1 && <button type="button" className="remove-q" onClick={() => setQuestions((x) => x.filter((_, i) => i !== qi))}>✕</button>}
                    </div>
                ))}
                <button type="button" className="add-question-btn" onClick={() => setQuestions([...questions, { text: "", type: "CHOIX", reponses: emptyReponses() }])}>+ Ajouter une question</button>
                {error && <p className="error">{error}</p>}
                <button type="submit" disabled={loading}>{loading ? "Création..." : "Créer le quiz"}</button>
            </form>
        </div>
    );
}
