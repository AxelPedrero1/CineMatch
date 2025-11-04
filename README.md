# CineMatch 🎬 Deluxe

## Objectif du projet

CineMatch est une application de recommandations de films avec interface Swing au look néon. Elle combine un service de suggestion propulsé par un modèle Ollama et une interface multi-outils permettant de trouver une œuvre similaire à un film aimé, de découvrir des pépites aléatoires, de gérer sa liste personnelle, de consulter l’historique et d’échanger avec un agent conversationnel spécialisé cinéma.

[App.java](src/main/java/app/cinematch/App.java) 
/
[MainFrame.java](src/main/java/app/cinematch/ui/swing/MainFrame.java)

**Image de la page d’accueil de l’application.**
![page_principale.png](documentations/images/page_principale.png)

## Fonctionnalités clés

- **Recommandations intelligentes** : génération d’idées à partir d’un film apprécié ou via un mode découverte aléatoire, avec enrichissement automatique (raison, plateforme, année).
- **Mode swipe** : interface inspirée d'un « tinder de films » pour accepter/refuser les propositions et enregistrer un statut en un clic.
- **Gestion de wishlist** : stockage persistant des films vus, à voir ou ignorés, consultable depuis l’outil « Ma liste ».
- **Chat IA outillé** : le panneau de discussion s’appuie sur LangChain4j pour orchestrer des outils (ajout multiple, génération de description, maintenance) tout en conservant une mémoire glissante de la conversation.
- **Persistance JSON** : toutes les actions utilisateur sont sérialisées dans `src/main/resources/storage.json`.


## Architecture logicielle

```
src/main/java/app/cinematch
├── App.java                      # Point d’entrée : FlatLaf + injection des services
├── MovieRecommenderService.java  # Prompts Ollama + persistance
├── agent/
│   ├── ChatAgent.java            # Orchestration locale + mémoire courte
│   ├── ConversationMemory.java
│   ├── Memory.java
│   ├── Profile.java
│   ├── langchain/
│   │   ├── CineAssistant.java        # Interface LangChain4j avec règles métiers
│   │   └── LangChain4jAgentBridge.java # Pont tools + heuristiques client
│   └── tools/
│       ├── WishlistTools.java        # CRUD wishlist / statuts
│       ├── LibraryTools.java         # Accès JsonStorage depuis l’agent
│       ├── ViewingTools.java         # Génération descriptions / next-to-watch
│       ├── MaintenanceTools.java
│       ├── BulkTools.java
│       └── MultiActionTools.java
├── api/OllamaClient.java          # Client HTTP pour le modèle Ollama
├── model/…                        # Records (Recommendation, HistoryEntry, …)
├── ui/swing/                      # Fenêtres/panneaux (Home, SimilarMoviePanel, History, etc..)
└── util/JsonStorage.java          # Persistance JSON thread-safe

```

- **App** initialise FlatLaf, configure le client Ollama (URL et modèle via variables d’environnement) et instancie la fenêtre principale.
[App.java](src/main/java/app/cinematch/App.java)
- **MovieRecommenderService** encapsule la génération de recommandations (à partir d’un film aimé ou aléatoires), les descriptions et l’enregistrement des statuts via un point d’injection persistant.
[MovieRecommenderService.java](src/main/java/app/cinematch/MovieRecommenderService.java)
- **ChatAgent** orchestre le dialogue avec le LLM pour le panneau de chat, s’appuyant sur une mémoire conversationnelle légère et un profil métier (expert cinéma).
[ChatAgent.java](src/main/java/app/cinematch/agent/ChatAgent.java)
- **Interface Swing** repose sur un `CardLayout` : `SimilarMoviePanel` (film similaire + description), `SwipeRecommenderPanel` (mode « swipe »), `WishlistPanel` (liste personnelle), `ChatPanel` (chat IA) et `HistoryPanel` (journal des interactions), retiré de l’interface finale car jugé non essentiel à l’usage principal.
[MainFrame.java](src/main/java/app/cinematch/ui/swing/MainFrame.java)
- **Utilitaires** : `JsonStorage` gère un fichier JSON persistant et `ImageLoader` centralise le chargement des visuels pour l’interface.
[JsonStorage.java](src/main/java/app/cinematch/util/JsonStorage.java)

## Installation et exécution

### Prérequis

- Java 17+
- Maven 3.9+
- Une instance [Ollama](https://ollama.ai/) accessible (locale ou distante)

### Étapes

1. **Cloner le dépôt** :
   ```bash
   git clone <url-du-depot>
   cd tsettssea
   ```
2. **Préparer Ollama** (dans un terminal séparé) :
   ```bash
   ollama pull qwen2.5:7b-instruct
   ollama serve
   ```
3. **Configurer les variables (optionnel)** :
   ```bash
   export OLLAMA_BASE_URL="http://localhost:11434"
   export OLLAMA_MODEL="qwen2.5:7b-instruct"
   ```
4. **Compiler et lancer l’application** :
   ```bash
   mvn clean package
   mvn exec:java -Dexec.mainClass=app.cinematch.App
   ```

**💡 Remarques :**

Assurez-vous que le serveur Ollama tourne en arrière-plan avant de lancer l’application (ollama serve doit rester ouvert).

Les commandes Maven utilisent les plugins déclarés dans le `pom.xml` (Checkstyle, SpotBugs, JaCoCo) pour vérifier la qualité du code et produire les rapports HTML.
[pom.xml](pom.xml)

## Modèle Ollama

L’application interroge par défaut le modèle **`qwen2.5:7b-instruct`**, configurable via la variable d’environnement `OLLAMA_MODEL`. Toutes les requêtes transitent par `OllamaClient`, basé sur LangChain4j pour la gestion des prompts et du streaming.
[App.java](src/main/java/app/cinematch/App.java) /
[OllamaClient.java](src/main/java/app/cinematch/api/OllamaClient.java)

## Répartition des tâches

**Commun**
- Mise en place de la **version initiale (V1)** du projet.
- Configuration des **dépendances Maven** et de l’environnement de développement.
- Création et paramétrage du **dépôt GitHub** pour le travail collaboratif.
- Installation d’**Ollama** et du **modèle de langage choisi (Gwen)**.
- Développement des **trois outils principaux** :
    -  Découverte aléatoire de films.
    -  Suggestion de films similaires à un titre donné.
    -  Consultation de l’historique et des avis enregistrés.

**Léo**
- IA & Agent : ajout de l’agent conversationnel (`feature/AgentMemory`) et de la **mémoire de conversation** (`feature/ConversationMemory`).
- UX Chat : **refonte visuelle de `ChatPanel`** et **barre de chargement** pendant la réflexion de l’IA (`feature/LoadingBar`).
- Maintenance : retrait de l’API TMDB ; adaptations des tests à la nouvelle UI du chat.

**Axel**
- Qualité & Tests : mise en place des **outils de qualité** (JaCoCo, SpotBugs, Checkstyle) et **tests JUnit** à large couverture :
    - UI : suites Swing robustes (EDT-safe, headless) pour `Les 4 Panels`, `History`, `Home`, `MainFrame`.
- Robustesse : nombreuses **corrections SpotBugs** (copies défensives, non-sérialisation de `SwingWorker`, formats portables).
- CI/Repo : ajustements de workflows/permissions et intégration continue orientée tests/qualité.

**Simon**
- UI/UX : **améliorations visuelles** (accueil, les 4 panels, description, swipe buttons) et **corrections SwipeRecommenderPanel**.
- Documentation : **Javadocs** sur `api`, `agent`, `model`, `uiSwing`, `util`.
- Qualité : corrections ciblées SpotBugs (dont `ChatAgent`), **coordination & merges** réguliers des PRs.

## Tests et qualité logicielle

La mise en place de tests approfondie permettant de garantir sa stabilité, sa robustesse et la conformité aux bonnes pratiques de développement.

### 🔹 Méthodologie
- Les tests unitaires ont été rédigés selon le **format BDD GIVEN / WHEN / THEN**, facilitant la lisibilité et la compréhension du comportement attendu.
- L’ensemble des tests a été implémenté avec **JUnit 5** et **Mockito** pour le mock des dépendances et la simulation des réponses du modèle Ollama.
- L’exécution et le suivi de la couverture sont assurés par **JaCoCo**, intégré au cycle Maven.

### 🔹 Couverture
- La couverture globale dépasse **90 %** sur l’ensemble du projet.
- Plusieurs modules atteignent **100 %** de couverture :
    - `MovieRecommenderService`
    - `OllamaClient`
    - `JsonStorage`
    - `ImageLoader`
- Des tests Swing spécifiques (EDT-safe, headless) ont été ajoutés pour valider la stabilité de l’interface utilisateur (`SimilarMoviePanel` à `ChatPanel`, `History`, `Home`, `MainFrame`).

### 🔹 Outils de qualité
- **JaCoCo** : mesure de couverture de code.
- **Checkstyle** : respect des conventions de code Java.
- **SpotBugs** : détection statique d’erreurs potentielles.

Ces outils garantissent un code maintenable, conforme aux standards et testable à long terme.

## Agent IA & LangChain4j

- `App` instancie un `LangChain4jAgentBridge` configuré sur Ollama (`OLLAMA_BASE_URL`, `OLLAMA_MODEL`) et l’injecte dans `ChatAgent` via un délégué fonctionnel.
[App.java](src/main/java/app/cinematch/App.java)
- Le bridge expose un contrat `CineAssistant` doté d’un prompt système contraignant l’usage des outils et la formulation des réponses.
[CineAssistant.java](src/main/java/app/cinematch/agent/langchain/CineAssistant.java)
- Des outils LangChain4j spécialisés traduisent les intentions en appels métier : ajout/suppression en masse, modifications de statut, statistiques, recommandations à regarder ensuite, etc.
[MultiActionTools.java](src/main/java/app/cinematch/agent/tools/MultiActionTools.java)
- Un pré-traitement côté client gère les commandes d’ajout multiple avant délégation au LLM, garantissant robustesse même hors connexion modèle.
[LangChain4jAgentBridge.java](src/main/java/app/cinematch/agent/langchain/LangChain4jAgentBridge.java)
