import type { Salon } from "../../types/salon";

interface SalonCardProps {
    salon: Salon;
}

export default function SalonCard({ salon }: SalonCardProps) {
    return (
        <article className="salon-card">
            <h2>Salon #{salon.code}</h2>

            <p>
                État : {salon.etat}
            </p>

            <p>
                Joueurs : {salon.users.length}
            </p>

            <ul>
                {salon.users.map((user) => (
                    <li key={user.id}>
                        {user.username}
                    </li>
                ))}
            </ul>
        </article>
    );
}