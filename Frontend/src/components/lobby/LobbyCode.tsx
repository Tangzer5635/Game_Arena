interface Props { code: string; }
export default function LobbyCode({ code }: Props) {
    const copy = () => navigator.clipboard.writeText(code).then(() => alert("Code copié !"));
    return (
        <div className="lobby-code-wrapper" onClick={copy} title="Cliquer pour copier">
            <div className="lobby-code">{code}</div>
            <div className="lobby-code-hint">📋 Cliquer pour copier</div>
        </div>
    );
}