angular.module('app', [
  'ngAnimate',
  'ngRoute',
  'ui.bootstrap'
])

.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/map', {
        templateUrl: 'map.html'
      }).
      otherwise({
        redirectTo: "/map"
      });
  }])

.controller('MapCtrl', [ '$scope', '$http', '$q',
  function($scope, $http, $q) {
    $scope.selected = {
      route: null,
      direction: null
    };

    $scope.$watchCollection('selected', function(selected) {
      if ( _.all(selected) ) {
        $http({
          url: '/stops',
          method: "GET",
          params: selected
        }).
        then(function(results) {
          $scope.stops = results.data;
        });
      }
    });

    $q.all([ $http.get("/routes"), $http.get("/directions") ]).
      then(function(results) {
        $scope.routes = results[0].data,
        $scope.directions = results[1].data;
      });
  }])
