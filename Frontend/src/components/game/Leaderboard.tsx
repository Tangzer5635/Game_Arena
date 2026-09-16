import type { GameScores } from "../../services/gameSocket";
interface Props { scores: GameScores; myName: string; }
export default function Leaderboard({ scores, myName }: Props) {
    const sorted = Object.entries(scores.scores).sort(([, a], [, b]) => b - a);
    return (
        <div className="live-leaderboard">
            <h3>Classement</h3>
            {sorted.map(([username, score], i) => (
                <div key={username} className={`leaderboard-row ${username === myName ? "me" : ""}`}>
                    <span className="lb-rank">{i === 0 ? "🥇" : i === 1 ? "🥈" : i === 2 ? "🥉" : i + 1}</span>
                    <span className="lb-name">{username}</span>
                    {(scores.streaks?.[username] ?? 0) >= 2 && <span className="lb-streak">🔥{scores.streaks[username]}</span>}
                    <span className="lb-score">{score} pts</span>
                </div>
            ))}
        </div>
    );
}