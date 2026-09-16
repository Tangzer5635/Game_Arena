const COLORS = [
    'linear-gradient(135deg,#55917F,#6BAB90)',
    'linear-gradient(135deg,#5E4C5A,#7a617a)',
    'linear-gradient(135deg,#7a5540,#b07850)',
    'linear-gradient(135deg,#3a5f6f,#4e7f90)',
];
function colorFor(username: string) {
    let h = 0;
    for (const c of username) h = c.charCodeAt(0) + ((h << 5) - h);
    return COLORS[Math.abs(h) % COLORS.length];
}
interface Props { username: string; size?: number; }
export default function PlayerAvatar({ username, size = 30 }: Props) {
    return (
        <span className="player-avatar"
            style={{ background: colorFor(username), width: size, height: size, fontSize: size * 0.42 }}>
            {username.slice(0, 2).toUpperCase()}
        </span>
    );
}