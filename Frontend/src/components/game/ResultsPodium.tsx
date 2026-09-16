import type { GameScores } from "../../services/gameSocket";
interface Props { finalScores: GameScores; myName: string; }
export default function ResultsPodium({ finalScores, myName }: Props) {
    const sorted = Object.entries(finalScores.scores).sort(([, a], [, b]) => b - a);
    return (
        <>
            <div className="podium">
                {sorted.slice(0, 3).map(([username, score], i) => (
                    <div key={username} className={`podium-place podium-${i + 1} ${username === myName ? "me" : ""}`} style={{ animationDelay: `${i * 0.15}s` }}>
                        <span className="podium-emoji">{["🥇","🥈","🥉"][i]}</span>
                        <span className="podium-name">{username}</span>
                        <span className="podium-score">{score} pts</span>
                    </div>
                ))}
            </div>
            {sorted.length > 3 && (
                <table className="scores-table" style={{ marginTop: 12 }}>
                    <tbody>
                        {sorted.slice(3).map(([username, score], i) => (
                            <tr key={username} className={username === myName ? "me-row" : ""}>
                                <td style={{ width: 32 }}>{i + 4}</td><td>{username}</td><td style={{ textAlign: "right" }}>{score} pts</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </>
    );
}