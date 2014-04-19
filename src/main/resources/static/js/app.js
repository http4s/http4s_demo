var phonecatApp = angular.module('phonecatApp', [
  'ngRoute',
  'mainApp'
]);

phonecatApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/home', {
        templateUrl: 'staticviews/partials/home.html'
      }).
      when('/phones', {
        templateUrl: 'staticviews/partials/phones.html',
        controller: 'PhoneListCtrl'
      }).
      when('/xhr', {
        templateUrl: 'staticviews/partials/xhr.html',
        controller: 'xhrController'
      }).
      when('/websocket', {
        templateUrl: 'staticviews/partials/websocket.html',
        controller: 'websocketController'
      }).
      when('/wschat', {
        templateUrl: 'staticviews/partials/chat.html',
        controller: 'wsChatController'
      }).
      otherwise({
        redirectTo: '/home'
      });
  }]);