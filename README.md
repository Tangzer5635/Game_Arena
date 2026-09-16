# Game Arena

![Game Arena](./frontend/public/favicon.ico)

**Game Arena** est une application web de quiz multijoueur permettant de créer des quiz, créer ou rejoindre un salon, puis jouer en temps réel avec plusieurs joueurs.

## Fonctionnalités

### Authentification
- Inscription et connexion
- Authentification JWT
- Rôles `USER` et `ADMIN`

### Quiz
- Consultation et création de quiz
- Ajout, modification et suppression de questions
- Modification du titre et de la description
- Questions `CHOIX` et `SAISIE_LIBRE`

### Jeu multijoueur
- Création et rejoint de salons par code
- Countdown
- Questions en temps réel
- Timer de 15 secondes
- Réponses QCM et saisie libre
- Correction automatique
- Calcul des points
- Séries et multiplicateurs
- Classement en direct
- Révélation des réponses
- Podium final
- Reconnexion à une partie en cours

## Architecture

```text
Game_Arena/
├── backend/
│   └── Spring Boot
│       ├── REST API
│       ├── Spring Security / JWT
│       ├── JPA / Hibernate
│       ├── PostgreSQL
│       └── WebSocket / STOMP
│
└── frontend/
    └── React + TypeScript
        ├── Pages
        ├── Components
        ├── Services
        ├── Context
        └── WebSocket client
```

## Backend

Technologies principales :

- Java 21
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- WebSocket
- STOMP
- SockJS

Serveur par défaut :

```text
http://localhost:8080
```

API :

```text
/api/v1
```

## Frontend

Technologies principales :

- React
- TypeScript
- Vite
- Axios
- React Router
- STOMP.js
- SockJS

WebSocket :

```text
/ws
```

## Types de questions

### `CHOIX`

Le joueur sélectionne une réponse parmi les propositions.

### `SAISIE_LIBRE`

Le joueur écrit sa réponse dans un champ texte et peut la valider avec le bouton ou avec la touche Entrée.

Pendant une partie WebSocket, une question libre est identifiée par :

```ts
saisieLibre: boolean;
```

L'envoi utilise :

```json
{
  "reponseId": null,
  "answer": "ma réponse"
}
```

## Installation

### Prérequis

- Java 21
- Node.js
- npm
- PostgreSQL

### Backend

```bash
./mvnw spring-boot:run
```

Sous Windows :

```bash
mvnw.cmd spring-boot:run
```

### Frontend

```bash
npm install
npm run dev
```

Le frontend est généralement disponible sur :

```text
http://localhost:5173
```

## Démarrage

1. Démarrer PostgreSQL.
2. Démarrer le backend sur le port `8080`.
3. Démarrer le frontend avec `npm run dev`.
4. Ouvrir l'application.
5. Se connecter ou créer un compte.
6. Créer ou sélectionner un quiz.
7. Créer un salon.
8. Rejoindre le salon avec les autres joueurs.
9. Lancer la partie.

## Structure frontend

```text
src/
├── components/
│   └── game/
│       ├── AnswerButton.tsx
│       ├── AnsweredIndicators.tsx
│       ├── GameTimer.tsx
│       ├── Leaderboard.tsx
│       └── ResultsPodium.tsx
├── context/
│   └── AuthContext.tsx
├── pages/
│   ├── Game.tsx
│   └── auth/
│       └── Login.tsx
├── services/
│   ├── api.ts
│   ├── authService.ts
│   ├── gameSocket.ts
│   ├── quizService.ts
│   ├── salonService.ts
│   ├── salonSocket.ts
│   └── userService.ts
└── types/
    ├── api.ts
    ├── auth.ts
    ├── quiz.ts
    ├── salon.ts
    └── user.ts
```

## Communication temps réel

Le jeu utilise WebSocket/STOMP pour transmettre notamment :

```text
question
scores
answer-result
reveal
results
countdown
```

Le client peut également demander une reconnexion à une partie en cours.

## Modèle de quiz

```ts
export type QuestionType = "CHOIX" | "SAISIE_LIBRE";

export interface Reponse {
    id: number;
    text: string;
    estBonne: boolean;
}

export interface Question {
    id: number;
    text: string;
    type: QuestionType;
    reponses: Reponse[];
}

export interface Quiz {
    id: number;
    titre: string;
    description: string;
    questions: Question[];
}
```

## Développement

Frontend :

```bash
npm run build
```

Backend :

```bash
./mvnw clean verify
```

Avant un commit, vérifier que le build frontend et le build backend passent sans erreur.

## Configuration

Les paramètres de connexion PostgreSQL sont définis dans la configuration Spring du backend (`application.properties` ou `application.yml`).

Les secrets et mots de passe ne doivent pas être commités dans Git.

## Dépannage

En cas de problème, vérifier :

- PostgreSQL est démarré ;
- le backend écoute sur `8080` ;
- l'URL de l'API est correcte ;
- le token JWT est valide ;
- le endpoint WebSocket `/ws` est accessible ;
- les logs Spring Boot ;
- la console du navigateur.

## Statut

Projet en développement.

Game Arena combine une API REST pour la gestion des utilisateurs, quiz et salons avec WebSocket/STOMP pour les parties multijoueurs en temps réel.
