from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import MealPostViewSet, SpotRatingViewSet

router = DefaultRouter()
router.register(r'posts', MealPostViewSet, basename='meal-post')
router.register(r'ratings', SpotRatingViewSet, basename='spot-rating')

urlpatterns = [
    path('', include(router.urls)),
]