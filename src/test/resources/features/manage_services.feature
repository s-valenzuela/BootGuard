Feature: Manage monitored services
  As an operator
  I want to register and remove monitored services from the dashboard
  So that I can track their health at a glance

  Scenario: Register a new service and see it on the dashboard
    Given I am on the services page
    When I add a service with URL "http://localhost:9999"
    Then a service card for "http://localhost:9999" should appear on the dashboard

  Scenario: Remove a registered service
    Given a service registered with URL "http://localhost:9999"
    And I am on the services page
    When I delete the service "http://localhost:9999"
    Then no service card for "http://localhost:9999" should appear on the dashboard
