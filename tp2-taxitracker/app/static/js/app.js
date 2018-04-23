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
    
    $scope.goToAdmin = function() {
      $location.path('/admin');
    };
    
    $http.get('/rest/admin_stats', query)
            .success(function(data, status, headers, config) {
                $scope.stats.guests.push(data);
            });
  
  });
  
  App.controller('AdminCtrl', function($scope, $rootScope, $log, $http, $routeParams, $location, $route) {
  
    $scope.filters = {
        taxi_id : null,
        date_from : null,
        date_to : null,
    }
    
    $scope.stats = [];

    $scope.submitInsert = function() {
        let query = {};
        for (filter in $scope.filters) {
            if ($scope.filters) {
                query[filter] = $scope.filters[filter]
            }
        }
        $http.get('/rest/admin_stats', query)
            .success(function(data, status, headers, config) {
                $scope.stats.guests.push(data);
            });
    }
  });
  