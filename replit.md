# Overview

IconeSign is a comprehensive electronic signature platform that provides XML digital signature services for invoice processing. The application offers a unified workflow combining TTN (Tunisian Tax Network) e-facturation integration and ANCE SEAL digital signature capabilities. Users can sign XML documents, save them to TTN systems, validate signatures, and download processed results in organized ZIP packages.

The platform features a modern Next.js frontend with an adorable UI/UX design, backed by a robust Spring Boot API that handles XML signing, validation, and TTN integration. The system is designed for secure deployment with only the frontend exposed to the internet while keeping backend services internal.

# User Preferences

Preferred communication style: Simple, everyday language.

# System Architecture

## Frontend Architecture
The frontend is built with **Next.js 14** using the App Router architecture and serves as the primary user interface. Key architectural decisions include:

- **Component-based Design**: Utilizes React Server Components and client components with TypeScript for type safety
- **State Management**: Implements Zustand for lightweight, scalable state management across the application  
- **UI Framework**: Built on Tailwind CSS with Radix UI components for accessibility and consistency
- **Animation System**: Framer Motion provides smooth transitions and micro-interactions
- **Theme System**: Custom "adorable" design system with gradient-based color schemes and glassmorphism effects
- **Development Server Configuration**: Runs on port 5000 with hostname binding for container compatibility

## Backend Architecture  
The backend follows a **Spring Boot** microservices pattern with clear separation of concerns:

- **REST API Design**: Implements RESTful endpoints under `/api/v1` context path for all operations
- **Service Layer Pattern**: Separate services for TTN integration, ANCE SEAL operations, and unified workflow processing
- **Database Layer**: PostgreSQL with JPA/Hibernate for data persistence and audit trail logging
- **File Processing**: Handles XML document uploads, signature generation, and ZIP package creation
- **Validation Engine**: EU DSS-based signature validation with comprehensive reporting

## API Integration Strategy
The system implements a **proxy pattern** for seamless frontend-backend communication:

- **Reverse Proxy**: Next.js rewrites `/api/*` requests to Spring Boot backend on port 8080
- **Context Path Mapping**: Frontend `/api` routes map to backend `/api/v1` endpoints
- **Environment Flexibility**: Supports both internal container networking and external API URLs
- **CORS Configuration**: Configured for cross-origin requests in development environments

## Authentication & Security
While not fully implemented in the current codebase, the architecture includes:

- **AuthGuard Component**: Wrapper component for protecting authenticated routes
- **Internal Network Security**: Backend and database services are isolated from public internet
- **File Upload Security**: Secure handling of XML documents with validation and sanitization

# External Dependencies

## Core Technologies
- **Next.js 14**: React framework with App Router for frontend development
- **Spring Boot**: Java backend framework for REST API development  
- **PostgreSQL**: Primary database for data persistence and audit logging
- **TypeScript**: Type safety across the frontend application

## UI/UX Libraries
- **Tailwind CSS**: Utility-first CSS framework for styling
- **Radix UI**: Accessible component primitives (@radix-ui/react-*)
- **Framer Motion**: Animation library for smooth transitions and interactions
- **Lucide React**: Icon library for consistent iconography
- **Sonner**: Toast notification system for user feedback

## Digital Signature Services
- **ANCE SEAL**: Tunisian digital signature service for XML document signing
- **EU DSS (Digital Signature Service)**: European standard for signature validation and verification
- **XAdES-B**: XML Advanced Electronic Signatures baseline profile implementation

## TTN Integration
- **TTN E-Facturation API**: Tunisian Tax Network integration for invoice submission and retrieval
- **XML Processing**: Custom XML parsing and transformation for invoice documents

## Development & Deployment
- **Docker**: Containerization for consistent deployment across environments
- **Kubernetes**: Container orchestration with internal service networking
- **Cloudflare Tunnel**: Secure external access to frontend without exposing backend services
- **Kind**: Kubernetes in Docker for local development environments

## State Management & HTTP
- **Zustand**: Lightweight state management for React applications
- **React Dropzone**: File upload handling with drag-and-drop functionality
- **Axios/Fetch**: HTTP client for API communication (implied from API patterns)

The architecture emphasizes security through network isolation, with only the frontend accessible via Cloudflare tunnel while maintaining internal service communication through container networking.