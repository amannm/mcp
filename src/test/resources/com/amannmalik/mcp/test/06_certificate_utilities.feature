@certificates
Feature: Certificate utilities
  # Verifies conformance to specification/2025-06-18/server/utilities/certificates.mdx
  As a developer
  I want to generate certificates for development
  So that I can test HTTPS locally

  Scenario: Generate self-signed certificate and CSR
    Given I generate an RSA key pair
    When I create a self-signed certificate for "example.com" with SAN "example.com"
    Then the certificate subject should be "CN=example.com"
    And the certificate should contain SAN "example.com"
    When I create a CSR for "example.com" with SAN "example.com"
    Then the CSR subject should be "CN=example.com"
