interface CardProps {
    title: string;
    description: string;
    buttonText?: string;
    onClick?: () => void;
}

export default function Card({
                                 title,
                                 description,
                                 buttonText,
                                 onClick,
                             }: CardProps) {
    return (
        <article className="card">
            <h2>{title}</h2>

            <p>{description}</p>

            {buttonText && (
                <button onClick={onClick}>
                    {buttonText}
                </button>
            )}
        </article>
    );
}