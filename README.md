# CptIA

## Arquitetura técnica de gamificação

- `user_actions` é a trilha canônica de eventos de gamificação (event sourcing), com ações como avaliação, check-in e mensagem.
- `users` guarda projeções agregadas derivadas dessa trilha para leitura rápida em tela, como `totalXp`, `ratedBooksCount`, `finishedBooksCount`, `groupMessageCount`, `readingCheckinCount` e `currentStreak`.
- Em caso de inconsistência de dados, use a rotina administrativa `GamificationService.recomputeUserProjection(userId)` (um usuário) ou `GamificationService.recomputeAllUsersProjections()` (todos os usuários) para recomputar as projeções diretamente a partir de `user_actions`.
