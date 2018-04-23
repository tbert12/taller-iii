App.config(function($routeProvider) {
  let views = "/view";
  $routeProvider.when('/', {
    controller : 'MainCtrl',
    templateUrl: views + '/main.html',
    //resolve    : { 'guestService': 'guestService' },
  });
  $routeProvider.when('/admin', {
    controller : 'AdminCtrl',
    templateUrl: views + '/admin.html',
  });
  $routeProvider.otherwise({
    redirectTo : '/'
  });
});

/*
App.config(function($httpProvider) {
  $httpProvider.interceptors.push('myHttpInterceptor');
});
*/
App.controller('MainCtrl', function($scope, $rootScope, $log, $http, $routeParams, $location, $route) {

  $scope.stats = [];
  $scope._end = false;
  $scope.goToAdmin = function() {
      $location.path('/admin');
  };
  $scope.page = 1;
  $scope.loadStats = function() {
      if ($scope._end)
          return;
      let query = { page : $scope.page }
      $http.get('/rest/stats', query)
          .success(function(data, status, headers, config) {
              $scope.stats.guests.push(data.stats);
              $scope.page += 1
              if (data._end) {
                  $scope._end = true;
              }
          });
  }

});

App.controller('AdminCtrl', function($scope, $rootScope, $log, $http, $routeParams, $location, $route) {

  $scope.filters = {
      taxi_id : null,
      date_from : null,
      date_to : null,
      page : 1
  }
  
  $scope._end = false;

  $scope.stats = [];

  $scope.loadAdminStats = function() {
      if ($scope._end) {
          return;
      }
      let query = {};
      for (filter in $scope.filters) {
          if ($scope.filters) {
              query[filter] = $scope.filters[filter]
          }
      }
      $http.get('/rest/admin_stats', query)
          .success(function(data, status, headers, config) {
              $scope.stats.guests.push(data.stats);
              $scope.filters.page += 1
              if (data._end) {
                  $scope._end = true;
              }
          });
  }
});

