import type { Quiz } from "../../types/quiz";
interface Props { quiz: Quiz; selected: boolean; onSelect: () => void; }
export default function QuizOptionCard({ quiz, selected, onSelect }: Props) {
    return (
        <div className={`quiz-option ${selected ? "selected" : ""}`} onClick={onSelect}>
            <h3>{quiz.titre}</h3>
            <p>{quiz.description}</p>
            <p className="quiz-option-count">{quiz.questions.length} question{quiz.questions.length > 1 ? "s" : ""}</p>
        </div>
    );
}