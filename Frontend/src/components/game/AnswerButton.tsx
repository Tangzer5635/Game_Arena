import type { GameReponse, GameAnswerResult } from "../../services/gameSocket";
interface Props {
    reponse: GameReponse;
    phase: string;
    selectedId: number | null;
    lastResult: GameAnswerResult | null;
    revealId: number | null;
    disabled: boolean;
    onClick: () => void;
}
export default function AnswerButton({ reponse, phase, selectedId, lastResult, revealId, disabled, onClick }: Props) {
    let cls = "answer-btn";
    if (phase === "reveal") {
        if (reponse.id === revealId) cls += " correct";
        else if (reponse.id === selectedId) cls += " wrong";
    } else if (phase === "answered" && lastResult) {
        if (reponse.id === lastResult.correctReponseId) cls += " correct";
        else if (reponse.id === selectedId && !lastResult.correct) cls += " wrong";
    } else if (reponse.id === selectedId) {
        cls += " selected";
    }
    return <button className={cls} onClick={onClick} disabled={disabled}>{reponse.text}</button>;
}