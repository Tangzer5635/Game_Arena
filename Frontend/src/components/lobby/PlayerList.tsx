import type { User } from "../../types/user";
import PlayerAvatar from "./PlayerAvatar";
interface Props { users: User[]; createurId: number; maxPlayers: number; }
export default function PlayerList({ users, createurId, maxPlayers }: Props) {
    return (
        <>
            <h2 style={{ marginTop: 24 }}>Joueurs ({users.length}/{maxPlayers})</h2>
            <ul className="player-list">
                {users.map(u => (
                    <li key={u.id} className="player-item">
                        <PlayerAvatar username={u.username} />
                        {u.id === createurId && <span className="crown">👑</span>}
                        {u.username}
                    </li>
                ))}
            </ul>
        </>
    );
}