import { Link } from "react-router-dom";

export default function NotFound() {
    return (
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <h1>404</h1>
            <p>Cette page n'existe pas.</p>
            <Link to="/"><button style={{ marginTop: 16 }}>Retour à l'accueil</button></Link>
        </div>
    );
}
