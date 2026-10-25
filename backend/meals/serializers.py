from rest_framework import serializers
from rest_framework_gis.serializers import GeoFeatureModelSerializer
from .models import MealPost, SpotRating

class SpotRatingSerializer(serializers.ModelSerializer):
    username = serializers.ReadOnlyField(source='user.username')

    class Meta:
        model = SpotRating
        fields = ['id', 'username', 'score', 'comment', 'created_at']

class MealPostSerializer(GeoFeatureModelSerializer):
    username = serializers.ReadOnlyField(source='user.username')
    average_rating = serializers.SerializerMethodField()
    ratings_count = serializers.SerializerMethodField()

    class Meta:
        model = MealPost
        geo_field = 'location'
        fields = ['id', 'username', 'meal_type', 'spot_name', 'image_url', 'created_at', 'average_rating', 'ratings_count']

    def get_average_rating(self, obj):
        ratings = obj.ratings.all()
        if not ratings.exists():
            return 0.0
        return round(sum(r.score for r in ratings) / ratings.count(), 1)

    def get_ratings_count(self, obj):
        return obj.ratings.count()