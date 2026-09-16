const TIMER_SECONDS = 15;
interface Props { timeLeft: number; }
export default function GameTimer({ timeLeft }: Props) {
    const pct = (timeLeft / TIMER_SECONDS) * 100;
    const urgent = timeLeft <= 5;
    return (
        <div>
            <div style={{ textAlign: "right" }}>
                <span className={`timer-text ${urgent ? "urgent" : ""}`}>{timeLeft}s</span>
            </div>
            <div className="timer-bar-track">
                <div className={`timer-bar-fill ${urgent ? "urgent" : ""}`} style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}