import type { GameScores } from "../../services/gameSocket";
interface Props { scores: GameScores; myName: string; }
export default function AnsweredIndicators({ scores, myName }: Props) {
    return (
        <div className="answered-indicators">
            {Object.keys(scores.scores).map(username => {
                const done = scores.answeredUsers?.includes(username);
                return (
                    <span key={username} className={`player-chip ${done ? "done" : ""} ${username === myName ? "me" : ""}`}>
                        {done ? "✓" : "…"} {username}
                    </span>
                );
            })}
        </div>
    );
}