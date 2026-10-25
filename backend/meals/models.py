from django.contrib.gis.db import models
from django.contrib.auth import get_user_model
from django.core.validators import MinValueValidator, MaxValueValidator

User = get_user_model()

class MealPost(models.Model):
    MEAL_TYPE_CHOICES = [
        ('breakfast', 'Breakfast'),
        ('lunch', 'Lunch'),
        ('dinner', 'Dinner'),
        ('snack', 'Street Snack'),
    ]

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='meal_posts')
    meal_type = models.CharField(max_length=20, choices=MEAL_TYPE_CHOICES)
    spot_name = models.CharField(max_length=255, blank=True)
    image_url = models.URLField(max_length=1000)  # Presigned S3 or Cloud URL
    location = models.PointField(srid=4326)  # Longitude / Latitude for spatial indexing
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.user.username} - {self.meal_type} at {self.created_at.strftime('%Y-%m-%d %H:%M')}"

class SpotRating(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    meal_post = models.ForeignKey(MealPost, on_delete=models.CASCADE, related_name='ratings')
    score = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(5)])
    comment = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'meal_post')