# common-utility

## Overview
The `common-utility` module is a shared Spring Boot library within the `agentic-agile-planner`
project. It provides reusable components, configurations and data transfer objects that are
shared across all agent modules, eliminating code duplication and ensuring consistency.

## Purpose
This module serves as the foundation layer for the multi-agent planning automation system.
It centralises all Jira REST API integration logic, shared data models and common
configurations in a single place so that agent modules can depend on it without
reimplementing common functionality.

## What This Module Contains

### Jira Integration
The Jira integration package provides all the components needed to communicate with the
Jira REST API. It includes the configuration properties class that binds Jira connection
details from the application properties file, a WebClient configuration that pre-configures
the HTTP client with Basic Authentication headers and the base URL, and a Jira client that
exposes methods to create tickets, fetch sprint history and update story points.

### Data Transfer Objects
The dto package contains all shared data objects that flow between agent modules during
the planning automation pipeline. These include the planning request and response objects
used by the orchestrator, the Jira ticket object returned after ticket creation, the sprint
story document used for vector store storage and the estimation result carrying story point
suggestions and reasoning.

## Module Details

- **Group ID** — com.naga.ai
- **Artifact ID** — common-utility
- **Spring Boot Version** — 3.5.11
- **Java Version** — 21
- **Port** — 8089
- **Context Path** — /common-utility

## Dependencies
This module depends on Spring Boot Starter Web for REST endpoint support and Spring Boot
Starter WebFlux for the reactive WebClient used in Jira API communication. All dependency
versions are managed by the parent pom.

## Configuration
This module requires the following properties to be defined in the application properties
file of any module that depends on it. These include the Jira base URL, email, API token,
project key and story points custom field identifier. Sensitive values such as the API
token and email are expected to be provided as environment variables.

## Used By
All agent modules in the `agentic-agile-planner` project depend on this module as a
shared library. The sprint planner agent uses the Jira client for ticket creation, the
estimation agent uses it for sprint history retrieval and story point updates, and all
agents share the data transfer objects defined here.