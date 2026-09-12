import { Link } from "react-router-dom";
import type { Quiz } from "../../types/quiz";

interface QuizCardProps {
    quiz: Quiz;
}

export default function QuizCard({ quiz }: QuizCardProps) {
    return (
        <article className="quiz-card">
            <h2>{quiz.titre}</h2>

            <p>{quiz.description}</p>

            <p>
                {quiz.questions.length} question
                {quiz.questions.length > 1 ? "s" : ""}
            </p>

            <Link to={`/quizzes/${quiz.id}`}>
                Voir le quiz
            </Link>
        </article>
    );
}