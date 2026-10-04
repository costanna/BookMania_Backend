import { defineRailway, github, preserve, project, service } from "railway/iac";

// Replaces railway.json (Railway's Config as Code, deprecated after
// 2026-12-01). Applied with `railway config apply` from this repo - it is
// NOT picked up on git push. Review `railway config plan` first.
//
// This repo owns only the backend service; the frontend repo has its own
// partial for BookMania_Frontend.
export const partial = "BookMania_Backend";

export default defineRailway(() => {
  const BookMania_Backend = service("BookMania_Backend", {
    source: github("costanna/BookMania_Backend"),
    build: { builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    deploy: {
      numReplicas: 1,
      // Restart policy is left to Railway's default (ON_FAILURE, max 10
      // retries - same as the old railway.json). Railway stores the default
      // as unset, so declaring it here would show as a change on every plan.
      healthcheckPath: "/actuator/health",
      healthcheckTimeout: 300,
      // Serverless: the service sleeps when idle (first request after that
      // takes a few seconds to wake it up).
      sleepApplication: true,
    },
    // Values live only in Railway (secrets) - preserve() keeps whatever is
    // set there instead of this file owning (and wiping) them.
    env: {
      DATABASE_URL: preserve(),
      JWT_SECRET_KEY: preserve(),
      JWT_EXPIRATION: preserve(),
      CORS_ALLOWED_ORIGINS: preserve(),
    },
  });
  return project("BookMania", {
    resources: [BookMania_Backend],
  });
});
